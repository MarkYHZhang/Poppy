/* 
 * ###################################################################
 * # This file has been modified in order to suit our project needs  #
 * # The original file is from https://github.com/sarfata/pi-blaster #
 * ###################################################################
 *
 * pi-blaster.c Multiple PWM for the Raspberry Pi
 * Copyright (c) 2013 Thomas Sarlandie <thomas@sarlandie.net>
 *
 * Based on the most excellent servod.c by Richard Hirst
 * Copyright (c) 2013 Richard Hirst <richardghirst@gmail.com>
 *
 * This program provides very similar functionality to servoblaster, except
 * that rather than implementing it as a kernel module, servod implements
 * the functionality as a usr space daemon.
 *
 */
/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
*/

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <getopt.h>
#include <errno.h>
#include <stdarg.h>
#include <stdint.h>
#include <signal.h>
#include <time.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/sysmacros.h>
#include <fcntl.h>
#include <sys/mman.h>
#include "mailbox.h"
#include "pi-blaster.h"

// Set num of possible PWM channels based on the known pins size.
uint8_t num_channels;

// channel_map array is not setup as empty to avoid locking all GPIO
// inputs as PWM, they are set on the fly by the pin param passed.
static uint8_t channel_map[MAX_CHANNELS];

#define DEVFILE_MBOX    "/dev/pi-blaster-mbox"
#define DEVFILE_VCIO    "/dev/vcio"

#define PAGE_SIZE       4096
#define PAGE_SHIFT      12

// CYCLE_TIME_US is the period of the PWM signal in us.
#define CYCLE_TIME_US   2000
// SAMPLE_US is the pulse width increment granularity, again in microseconds.
// Setting SAMPLE_US too low will likely cause problems as the DMA controller
// will use too much memory bandwidth.  10us is a good value, though you
// might be ok setting it as low as 2us.
#define SAMPLE_US   1
#define NUM_SAMPLES (CYCLE_TIME_US/SAMPLE_US)
#define NUM_CBS     (NUM_SAMPLES*2)

#define NUM_PAGES   ((NUM_CBS * sizeof(dma_cb_t) + NUM_SAMPLES * 4 + \
                      PAGE_SIZE - 1) >> PAGE_SHIFT)

#define DMA_BASE        (periph_virt_base + 0x00007000)
#define DMA_CHAN_SIZE    0x100 /* size of register space for a single DMA channel */
#define DMA_CHAN_MAX    14  /* number of DMA Channels we have... actually, there are 15... but channel fifteen is mapped at a different DMA_BASE, so we leave that one alone */
#define DMA_CHAN_NUM    14  /* the DMA Channel we are using, NOTE: DMA Ch 0 seems to be used by X... better not use it ;) */
#define PWM_BASE_OFFSET 0x0020C000
#define PWM_BASE        (periph_virt_base + PWM_BASE_OFFSET)
#define PWM_PHYS_BASE   (periph_phys_base + PWM_BASE_OFFSET)
#define PWM_LEN         0x28
#define CLK_BASE_OFFSET 0x00101000
#define CLK_BASE        (periph_virt_base + CLK_BASE_OFFSET)
#define CLK_LEN         0xA8
#define GPIO_BASE_OFFSET  0x00200000
#define GPIO_BASE       (periph_virt_base + GPIO_BASE_OFFSET)
#define GPIO_PHYS_BASE  (periph_phys_base + GPIO_BASE_OFFSET)
#define GPIO_LEN        0x100
#define PCM_BASE_OFFSET 0x00203000
#define PCM_BASE        (periph_virt_base + PCM_BASE_OFFSET)
#define PCM_PHYS_BASE   (periph_phys_base + PCM_BASE_OFFSET)
#define PCM_LEN         0x24

#define DMA_NO_WIDE_BURSTS  (1<<26)
#define DMA_WAIT_RESP       (1<<3)
#define DMA_D_DREQ          (1<<6)
#define DMA_PER_MAP(x)      ((x)<<16)
#define DMA_END             (1<<1)
#define DMA_RESET           (1<<31)
#define DMA_INT             (1<<2)

#define DMA_CS              (0x00/4)
#define DMA_CONBLK_AD       (0x04/4)
#define DMA_DEBUG           (0x20/4)

#define GPIO_FSEL0          (0x00/4)
#define GPIO_SET0           (0x1c/4)
#define GPIO_CLR0           (0x28/4)
#define GPIO_LEV0           (0x34/4)
#define GPIO_PULLEN         (0x94/4)
#define GPIO_PULLCLK        (0x98/4)

#define GPIO_MODE_IN        0
#define GPIO_MODE_OUT       1

#define PWM_CTL     (0x00/4)
#define PWM_DMAC    (0x08/4)
#define PWM_RNG1    (0x10/4)
#define PWM_FIFO    (0x18/4)

#define PWMCLK_CNTL  40
#define PWMCLK_DIV   41

#define PWMCTL_MODE1  (1<<1)
#define PWMCTL_PWEN1  (1<<0)
#define PWMCTL_CLRF   (1<<6)
#define PWMCTL_USEF1  (1<<5)

#define PWMDMAC_ENAB    (1<<31)
#define PWMDMAC_THRSHLD ((15<<8)|(15<<0))

#define PCM_CS_A      (0x00/4)
#define PCM_FIFO_A    (0x04/4)
#define PCM_MODE_A    (0x08/4)
#define PCM_RXC_A     (0x0c/4)
#define PCM_TXC_A     (0x10/4)
#define PCM_DREQ_A    (0x14/4)
#define PCM_INTEN_A   (0x18/4)
#define PCM_INT_STC_A (0x1c/4)
#define PCM_GRAY      (0x20/4)

#define PCMCLK_CNTL   38
#define PCMCLK_DIV    39

#define DELAY_VIA_PWM 0
#define DELAY_VIA_PCM 1

/* New Board Revision format:
SRRR MMMM PPPP TTTT TTTT VVVV

S scheme (0=old, 1=new)
R RAM (0=256, 1=512, 2=1024)
M manufacturer (0='SONY',1='EGOMAN',2='EMBEST',3='UNKNOWN',4='EMBEST')
P processor (0=2835, 1=2836)
T type (0='A', 1='B', 2='A+', 3='B+', 4='Pi 2 B', 5='Alpha', 6='Compute Module')
V revision (0-15)
*/
#define BOARD_REVISION_SCHEME_MASK (0x1 << 23)
#define BOARD_REVISION_SCHEME_OLD (0x0 << 23)
#define BOARD_REVISION_SCHEME_NEW (0x1 << 23)
#define BOARD_REVISION_RAM_MASK (0x7 << 20)
#define BOARD_REVISION_MANUFACTURER_MASK (0xF << 16)
#define BOARD_REVISION_MANUFACTURER_SONY (0 << 16)
#define BOARD_REVISION_MANUFACTURER_EGOMAN (1 << 16)
#define BOARD_REVISION_MANUFACTURER_EMBEST (2 << 16)
#define BOARD_REVISION_MANUFACTURER_UNKNOWN (3 << 16)
#define BOARD_REVISION_MANUFACTURER_EMBEST2 (4 << 16)
#define BOARD_REVISION_PROCESSOR_MASK (0xF << 12)
#define BOARD_REVISION_PROCESSOR_2835 (0 << 12)
#define BOARD_REVISION_PROCESSOR_2836 (1 << 12)
#define BOARD_REVISION_TYPE_MASK (0xFF << 4)
#define BOARD_REVISION_TYPE_PI1_A (0 << 4)
#define BOARD_REVISION_TYPE_PI1_B (1 << 4)
#define BOARD_REVISION_TYPE_PI1_A_PLUS (2 << 4)
#define BOARD_REVISION_TYPE_PI1_B_PLUS (3 << 4)
#define BOARD_REVISION_TYPE_PI2_B (4 << 4)
#define BOARD_REVISION_TYPE_ALPHA (5 << 4)
#define BOARD_REVISION_TYPE_PI3_B (8 << 4)
#define BOARD_REVISION_TYPE_PI3_BP (0xD << 4)
#define BOARD_REVISION_TYPE_CM (6 << 4)
#define BOARD_REVISION_TYPE_CM3 (10 << 4)
#define BOARD_REVISION_REV_MASK (0xF)

#define BUS_TO_PHYS(x) ((x)&~0xC0000000)

#ifdef DEBUG
#define dprintf(...) printf(__VA_ARGS__)
#else
#define dprintf(...)
#endif

static struct {
	int handle;		/* From mbox_open() */
	unsigned mem_ref;	/* From mem_alloc() */
	unsigned bus_addr;	/* From mem_lock() */
	uint8_t *virt_addr;	/* From mapmem() */
} mbox;

typedef struct {
	uint32_t info, src, dst, length,
		 stride, next, pad[2];
} dma_cb_t;

struct ctl {
	uint32_t sample[NUM_SAMPLES];
	dma_cb_t cb[NUM_CBS];
};

static uint32_t periph_virt_base;
static uint32_t periph_phys_base;
static uint32_t mem_flag;

static volatile uint32_t *pwm_reg;
static volatile uint32_t *pcm_reg;
static volatile uint32_t *clk_reg;
static volatile uint32_t *dma_virt_base; /* base address of all DMA Channels */
static volatile uint32_t *dma_reg; /* pointer to the DMA Channel registers we are using */
static volatile uint32_t *gpio_reg;

static int delay_hw = DELAY_VIA_PWM;

uint32_t pwm[MAX_CHANNELS];

static void fatal(char *fmt, ...);

// open a char device file used for communicating with kernel mbox driver
int mbox_open() {
	int file_desc;

	// try to use /dev/vcio first (kernel 4.1+)
	file_desc = open(DEVFILE_VCIO, 0);
	if (file_desc < 0) {
		/* initialize mbox */
		unlink(DEVFILE_MBOX);
		if (mknod(DEVFILE_MBOX, S_IFCHR|0600, makedev(MAJOR_NUM, 0)) < 0)
				fatal("Failed to create mailbox device\n");
		file_desc = open(DEVFILE_MBOX, 0);
		if (file_desc < 0) {
				printf("Can't open device file: %s\n", DEVFILE_MBOX);
				perror(NULL);
				exit(-1);
		}
	}
	return file_desc;
}

void mbox_close(int file_desc) {
	close(file_desc);
}

static void gpio_set_mode(uint32_t pin, uint32_t mode) {
	uint32_t fsel = gpio_reg[GPIO_FSEL0 + pin / 10];

	  fsel &= ~(7 << ((pin % 10) * 3));
	  fsel |= mode << ((pin % 10) * 3);
	  gpio_reg[GPIO_FSEL0 + pin/10] = fsel;
}

static void udelay(int us) {
	struct timespec ts = { 0, us * 1000 };

	nanosleep(&ts, NULL);
}

static void terminate(int dummy) {
	int i;

	dprintf("Resetting DMA...\n");
	if (dma_reg && mbox.virt_addr) {
		for (i = 0; i < num_channels; i++)
			pwm[i] = 0;
		pwm_update();
		udelay(CYCLE_TIME_US);
		dma_reg[DMA_CS] = DMA_RESET;
		udelay(10);
	}

	dprintf("Freeing mbox memory...\n");
	if (mbox.virt_addr != NULL) {
		unmapmem(mbox.virt_addr, NUM_PAGES * PAGE_SIZE);
		if (mbox.handle <= 2) {
			/* we need to reopen mbox file */
			mbox.handle = mbox_open();
		}
		mem_unlock(mbox.handle, mbox.mem_ref);
		mem_free(mbox.handle, mbox.mem_ref);
		mbox_close(mbox.handle);
	}
	dprintf("Unlink %s...\n", DEVFILE_MBOX);
	unlink(DEVFILE_MBOX);
	printf("pi-blaster stopped.\n");

	exit(1);
}

static void fatal(char *fmt, ...) {
	va_list ap;

	va_start(ap, fmt);
	vfprintf(stderr, fmt, ap);
	va_end(ap);
	terminate(0);
}

/*
 * determine which pi model we are running on
 */
static void get_model(unsigned mbox_board_rev) {
	int board_model = 0;

	if ((mbox_board_rev & BOARD_REVISION_SCHEME_MASK) == BOARD_REVISION_SCHEME_NEW) {
		if ((mbox_board_rev & BOARD_REVISION_TYPE_MASK) == BOARD_REVISION_TYPE_PI2_B) {
			board_model = 2;
		} else if ((mbox_board_rev & BOARD_REVISION_TYPE_MASK) == BOARD_REVISION_TYPE_PI3_B) {
			board_model = 3;
		} else if ((mbox_board_rev & BOARD_REVISION_TYPE_MASK) == BOARD_REVISION_TYPE_PI3_BP) {
			board_model = 3;
		} else if ((mbox_board_rev & BOARD_REVISION_TYPE_MASK) == BOARD_REVISION_TYPE_CM3) {
			board_model = 3;
		} else {
			// no Pi 2, we assume a Pi 1
			board_model = 1;
		}
	} else {
		// if revision scheme is old, we assume a Pi 1
		board_model = 1;
	}

	switch(board_model) {
		case 1:
			periph_virt_base = 0x20000000;
			periph_phys_base = 0x7e000000;
			mem_flag = MEM_FLAG_L1_NONALLOCATING | MEM_FLAG_ZERO;
			break;
		case 2:
		case 3:
			periph_virt_base = 0x3f000000;
			periph_phys_base = 0x7e000000;
			mem_flag = MEM_FLAG_L1_NONALLOCATING | MEM_FLAG_ZERO;
			break;
		default:
			fatal("Unable to detect Board Model from board revision: %#x", mbox_board_rev);
			break;
	}
}

static uint32_t mem_virt_to_phys(void *virt) {
	uint32_t offset = (uint8_t *)virt - mbox.virt_addr;

	return mbox.bus_addr + offset;
}

static void* map_peripheral(uint32_t base, uint32_t len) {
	int fd = open("/dev/mem", O_RDWR | O_SYNC);
	void * vaddr;

	if (fd < 0)
		fatal("pi-blaster: Failed to open /dev/mem: %m\n");
	vaddr = mmap(NULL, len, PROT_READ|PROT_WRITE, MAP_SHARED, fd, base);
	if (vaddr == MAP_FAILED)
		fatal("pi-blaster: Failed to map peripheral at 0x%08x: %m\n", base);
	close(fd);

	return vaddr;
}

/*
 * What we need to do here is:
 *	 First DMA command turns on the pins that are >0
 *	 All the other packets turn off the pins that are not used
 *
 * For the cpb packets (The DMA control packet)
 *	-> cbp[0]->dst = gpset0: set	 the pwms that are active
 *	-> cbp[*]->dst = gpclr0: clear when the sample has a value
 *
 * For the samples		 (The value that is written by the DMA command to cbp[n]->dst)
 *	-> dp[0] = mask of the pwms that are active
 *	-> dp[n] = mask of the pwm to stop at time n
 *
 * We dont really need to reset the cb->dst each time but I believe it helps a lot
 * in code readability in case someone wants to generate more complex signals.
 */
void pwm_update() {
	uint32_t phys_gpclr0 = GPIO_PHYS_BASE + 0x28;
	uint32_t phys_gpset0 = GPIO_PHYS_BASE + 0x1c;
	struct ctl *ctl = (struct ctl *)mbox.virt_addr;
	uint32_t mask;

	/* First we turn on the channels that need to be on */
	/*	 Take the first DMA Packet and set it's target to start pulse */
	ctl->cb[0].dst = phys_gpset0;

	/*	 Now create a mask of all the pins that should be on */
	mask = 0;
	for (uint8_t i = 0; i < num_channels; i++) {
		// Check the channel_map pin has been set to avoid locking all of them as PWM.
		if (pwm[i] > 0)
			mask |= 1 << channel_map[i];
	}
	/*	 And give that to the DMA controller to write */
	ctl->sample[0] = mask;

	/* Now we go through all the samples and turn the pins off when needed */
	for (uint32_t j = 1; j < NUM_SAMPLES; j++) {
		ctl->cb[j*2].dst = phys_gpclr0;
		mask = 0;
		for (uint8_t i = 0; i < num_channels; i++) {
			// Check the channel_map pin has been set to avoid locking all of them as PWM.
			if (j >= pwm[i])
				mask |= 1 << channel_map[i];
		}
		ctl->sample[j] = mask;
	}
}

static void setup_sighandlers(void) {
	// Catch all signals possible - it is vital we kill the DMA engine
	// on process exit!
	for (int i = 0; i < 64; i++) {
		struct sigaction sa;

		memset(&sa, 0, sizeof(sa));
		sa.sa_handler = terminate;
		sigaction(i, &sa, NULL);
	}
	signal(SIGWINCH, SIG_IGN);
	signal(SIGPIPE, SIG_IGN);
}

static void init_ctrl_data(void) {
	dprintf("Initializing DMA ...\n");
	struct ctl *ctl = (struct ctl *)mbox.virt_addr;
	dma_cb_t *cbp = ctl->cb;
	uint32_t phys_fifo_addr;
	uint32_t phys_gpclr0 = GPIO_PHYS_BASE + 0x28;
	uint32_t phys_gpset0 = GPIO_PHYS_BASE + 0x1c;
	uint32_t mask;

	if (delay_hw == DELAY_VIA_PWM) {
		phys_fifo_addr = PWM_PHYS_BASE + 0x18;
	}
	else {
		phys_fifo_addr = PCM_PHYS_BASE + 0x04;
	}
	memset(ctl->sample, 0, sizeof(ctl->sample));

	// Calculate a mask to turn off all the servos
	mask = 0;
	for (uint8_t i = 0; i < num_channels; i++) {
		mask |= 1 << channel_map[i];
	}
	for (uint32_t i = 0; i < NUM_SAMPLES; i++) {
		ctl->sample[i] = mask;
	}

	/* Initialize all the DMA commands. They come in pairs.
	 *	- 1st command copies a value from the sample memory to a destination
	 *		address which can be either the gpclr0 register or the gpset0 register
	 *	- 2nd command waits for a trigger from an external source (PWM or PCM)
	 */
	for (uint32_t i = 0; i < NUM_SAMPLES; i++) {
		/* First DMA command */
		cbp->info = DMA_NO_WIDE_BURSTS | DMA_WAIT_RESP;
		cbp->src = mem_virt_to_phys(ctl->sample + i);
		cbp->dst = phys_gpclr0;
		cbp->length = 4;
		cbp->stride = 0;
		cbp->next = mem_virt_to_phys(cbp + 1);
		cbp++;
		/* Second DMA command */
		if (delay_hw == DELAY_VIA_PWM)
			cbp->info = DMA_NO_WIDE_BURSTS | DMA_WAIT_RESP | DMA_D_DREQ | DMA_PER_MAP(5);
		else
			cbp->info = DMA_NO_WIDE_BURSTS | DMA_WAIT_RESP | DMA_D_DREQ | DMA_PER_MAP(2);
		cbp->src = mem_virt_to_phys(ctl);	// Any data will do
		cbp->dst = phys_fifo_addr;
		cbp->length = 4;
		cbp->stride = 0;
		cbp->next = mem_virt_to_phys(cbp + 1);
		cbp++;
	}
	cbp--;
	cbp->next = mem_virt_to_phys(ctl->cb);
}

static void init_hardware(void) {
	dprintf("Initializing PWM/PCM HW...\n");
	struct ctl *ctl = (struct ctl *)mbox.virt_addr;
	if (delay_hw == DELAY_VIA_PWM) {
		// Initialise PWM
		pwm_reg[PWM_CTL] = 0;
		udelay(10);
		clk_reg[PWMCLK_CNTL] = 0x5A000006;		// Source=PLLD (500MHz)
		udelay(100);
		clk_reg[PWMCLK_DIV] = 0x5A000000 | (500<<12);	// set pwm div to 500, giving 1MHz
		udelay(100);
		clk_reg[PWMCLK_CNTL] = 0x5A000016;		// Source=PLLD and enable
		udelay(100);
		pwm_reg[PWM_RNG1] = SAMPLE_US;
		udelay(10);
		pwm_reg[PWM_DMAC] = PWMDMAC_ENAB | PWMDMAC_THRSHLD;
		udelay(10);
		pwm_reg[PWM_CTL] = PWMCTL_CLRF;
		udelay(10);
		pwm_reg[PWM_CTL] = PWMCTL_USEF1 | PWMCTL_PWEN1;
		udelay(10);
	} else {
		// Initialise PCM
		pcm_reg[PCM_CS_A] = 1;				// Disable Rx+Tx, Enable PCM block
		udelay(100);
		clk_reg[PCMCLK_CNTL] = 0x5A000006;		// Source=PLLD (500MHz)
		udelay(100);
		clk_reg[PCMCLK_DIV] = 0x5A000000 | (500<<12);	// Set pcm div to 500, giving 1MHz
		udelay(100);
		clk_reg[PCMCLK_CNTL] = 0x5A000016;		// Source=PLLD and enable
		udelay(100);
		pcm_reg[PCM_TXC_A] = 0<<31 | 1<<30 | 0<<20 | 0<<16; // 1 channel, 8 bits
		udelay(100);
		pcm_reg[PCM_MODE_A] = (SAMPLE_US - 1) << 10;
		udelay(100);
		pcm_reg[PCM_CS_A] |= 1<<4 | 1<<3;		// Clear FIFOs
		udelay(100);
		pcm_reg[PCM_DREQ_A] = 64<<24 | 64<<8;		// DMA Req when one slot is free?
		udelay(100);
		pcm_reg[PCM_CS_A] |= 1<<9;			// Enable DMA
		udelay(100);
	}

	// Initialise the DMA
	dma_reg[DMA_CS] = DMA_RESET;
	udelay(10);
	dma_reg[DMA_CS] = DMA_INT | DMA_END;
	dma_reg[DMA_CONBLK_AD] = mem_virt_to_phys(ctl->cb);
	dma_reg[DMA_DEBUG] = 7; // clear debug error flags
	dma_reg[DMA_CS] = 0x10880001;	// go, mid priority, wait for outstanding writes

	if (delay_hw == DELAY_VIA_PCM) {
		pcm_reg[PCM_CS_A] |= 1<<2;			// Enable Tx
	}
}

static void debug_dump_hw(void) {
#ifdef DEBUG
		printf("pwm_reg: %p\n", (void *) pwm_reg);

		struct ctl *ctl = (struct ctl *)mbox.virt_addr;
		dma_cb_t *cbp = ctl->cb;

		for (int i = 0; i < NUM_SAMPLES; i++) {
			printf("DMA Control Block: #%d @0x%08x, \n", i, cbp);
			printf("info:	 0x%08x\n", cbp->info);
			printf("src:		0x%08x\n", cbp->src);
			printf("dst:		0x%08x\n", cbp->dst);
			printf("length: 0x%08x\n", cbp->length);
			printf("stride: 0x%08x\n", cbp->stride);
			printf("next:	 0x%08x\n", cbp->next);
			cbp++; // next control block
		}
		printf("PWM_BASE: %p\n", (void *) PWM_BASE);
		printf("pwm_reg: %p\n", (void *) pwm_reg);
		for (int i=0; i<PWM_LEN/4; i++) {
			printf("%04x: 0x%08x 0x%08x\n", i, &pwm_reg[i], pwm_reg[i]);
		}
		printf("CLK_BASE: %p\n", (void *) CLK_BASE);
		printf("PWMCLK_CNTL: %x\n", PWMCLK_CNTL);
		printf("clk_reg[PWMCLK_CNTL]: %p\n", &clk_reg[PWMCLK_CNTL]);
		printf("PWMCLK_DIV: %x\n", PWMCLK_DIV);
		printf("clk_reg: %p\n", (void *) clk_reg);
		printf("virt_to_phys(clk_reg): %x\n", mem_virt_to_phys(clk_reg));
		for (int i=0; i<CLK_LEN/4; i++) {
			printf("%04x: 0x%08x 0x%08x\n", i, &clk_reg[i], clk_reg[i]);
		}
		printf("DMA_BASE: %p\n", (void *) DMA_BASE);
		printf("dma_virt_base: %p\n", (void *) dma_virt_base);
		printf("dma_reg: %p\n", (void *) dma_reg);
		printf("virt_to_phys(dma_reg): %x\n", mem_virt_to_phys(dma_reg));
		for (int i=0; i<DMA_CHAN_SIZE/4; i++) {
			printf("%04x: 0x%08x 0x%08x\n", i, &dma_reg[i], dma_reg[i]);
		}
#endif
}

static void debug_dump_samples() {
	struct ctl *ctl = (struct ctl *)mbox.virt_addr;

	for (int i = 0; i < NUM_SAMPLES; i++) {
		printf("#%d @0x%08x, \n", i, ctl->sample[i]);
	}
}

void pwm_init(uint8_t *channel_pins, uint8_t nchannels, int use_pcm) {

	num_channels = nchannels;

	memset(pwm, 0, sizeof(pwm));
	memcpy(channel_map, channel_pins, nchannels);
	delay_hw = use_pcm;

	mbox.handle = mbox_open();
	if (mbox.handle < 0)
		fatal("Failed to open mailbox\n");
	unsigned mbox_board_rev = get_board_revision(mbox.handle);
	printf("MBox Board Revision: %#x\n", mbox_board_rev);
	get_model(mbox_board_rev);
	unsigned mbox_dma_channels = get_dma_channels(mbox.handle);
	printf("DMA Channels Info: %#x, using DMA Channel: %d\n", mbox_dma_channels, DMA_CHAN_NUM);

	printf("Using hardware:                 %5s\n", delay_hw == DELAY_VIA_PWM ? "PWM" : "PCM");
	printf("Number of channels:             %5hhu\n", nchannels);
	printf("PWM frequency:               %5d Hz\n", 1000000/CYCLE_TIME_US);
	printf("PWM steps:                      %5d\n", NUM_SAMPLES);
	printf("Maximum period:               %5dus\n", CYCLE_TIME_US);
	printf("Minimum period:               %5dus\n", SAMPLE_US);
	printf("DMA Base:                  %#010x\n", DMA_BASE);

	setup_sighandlers();

	/* map the registers for all DMA Channels */
	dma_virt_base = map_peripheral(DMA_BASE, (DMA_CHAN_SIZE * (DMA_CHAN_MAX + 1)));
	/* set dma_reg to point to the DMA Channel we are using */
	dma_reg = dma_virt_base + DMA_CHAN_NUM * (DMA_CHAN_SIZE / sizeof(dma_reg));
	pwm_reg = map_peripheral(PWM_BASE, PWM_LEN);
	pcm_reg = map_peripheral(PCM_BASE, PCM_LEN);
	clk_reg = map_peripheral(CLK_BASE, CLK_LEN);
	gpio_reg = map_peripheral(GPIO_BASE, GPIO_LEN);

	/* Use the mailbox interface to the VC to ask for physical memory */
	mbox.mem_ref = mem_alloc(mbox.handle, NUM_PAGES * PAGE_SIZE, PAGE_SIZE, mem_flag);
	/* TODO: How do we know that succeeded? */
	dprintf("mem_ref %u\n", mbox.mem_ref);
	mbox.bus_addr = mem_lock(mbox.handle, mbox.mem_ref);
	dprintf("bus_addr = %#x\n", mbox.bus_addr);
	mbox.virt_addr = mapmem(BUS_TO_PHYS(mbox.bus_addr), NUM_PAGES * PAGE_SIZE);
	dprintf("virt_addr %p\n", mbox.virt_addr);

	if ((unsigned long)mbox.virt_addr & (PAGE_SIZE-1))
		fatal("pi-blaster: Virtual address is not page aligned\n");

	/* we are done with the mbox */
	mbox_close(mbox.handle);
	mbox.handle = -1;

	init_ctrl_data();
	init_hardware();

	for(int i = 0; i < nchannels; ++i) {
		gpio_set_mode(channel_pins[i], GPIO_MODE_OUT);
	}

	pwm_update();
}

/* int main(int argc, char **argv) {

	uint8_t pins[] = {12, 13, 19, 26};

	init_gpio(pins, 4, 0);

	while(1) {
		pwm[0] = 1000;
		pwm[1] = 0;
		pwm_update();
		udelay(5000);
		pwm[0] = 100;
		pwm_update();
		sleep(1);
		pwm[0] = 0;
		pwm[1] = 1000;
		pwm_update();
		udelay(5000);
		pwm[1] = 100;
		pwm_update();
		sleep(1);
		pwm[2] = 1000;
		pwm[3] = 0;
		pwm_update();
		udelay(5000);
		pwm[2] = 100;
		pwm_update();
		sleep(1);
		pwm[2] = 0;
		pwm[3] = 1000;
		pwm_update();
		udelay(5000);
		pwm[3] = 100;
		pwm_update();
		sleep(1);
	}

	return 0;
} */
