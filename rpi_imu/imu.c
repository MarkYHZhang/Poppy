#include <unistd.h>
#include <fcntl.h>
#include <stdint.h>
#include <time.h>
#include <math.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>
#include <linux/spi/spidev.h>
#include <sys/timerfd.h>
#include <sys/stat.h>
#include <string.h>

#include <stdio.h>

//#define USE_I2C

#define I2C_DEV "/dev/i2c-1"
#define SPI_DEV "/dev/spidev0.1"
#define AG_ADDR 0x68
#define MA_ADDR 0x0C

#define STRIFY(x) #x
#define STR(x) STRIFY(x)

#define TORADIAN M_PI / 180.0

#define EPS 1e-5

double accScale[4] = {2.0 / 32768.0, 4.0 / 32768.0, 8.0 / 32768.0, 16.0 / 32768.0};
double gyrScale[4] = {250.0 / 32768.0, 500.0 / 32768.0, 1000.0 / 32768.0, 2000.0 / 32768.0};
double magScale = 0.15 * 1e-3;

uint8_t accSens = 0;
uint8_t gyrSens = 0;

double tempSensitivity = 333.87;

// double upLimit = 65536;
// double dnLimit = 0;
// 
// uint32_t upShiftCount = 1;
// uint32_t dnShiftCount = 1000;

// uint32_t accUpCtr = 0;
// uint32_t accDnCtr = 0;
// uint32_t gyrUpCtr = 0;
// uint32_t gyrDnCtr = 0;

// uint64_t calibCtr = 0;

// uint8_t isCalib = 0;

struct vec {
	double x;
	double y;
	double z;
	double w;
};

struct mat {
	struct vec x;
	struct vec y;
	struct vec z;
	struct vec w;
};

struct calib_data {
	struct mat accCalib[4];
	struct mat gyrCalib[4];
	struct mat magCalib;
};

struct vec rot = {0, 0, 0, 1};

#define VEC_UP (struct vec){0, 0, 1, 1}
#define VEC_ZERO (struct vec){0, 0, 0, 1}
#define IDENTITY (struct mat){{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}}
struct calib_data calibOff = {{IDENTITY, IDENTITY, IDENTITY, IDENTITY}, {IDENTITY, IDENTITY, IDENTITY, IDENTITY}, IDENTITY};

struct vec cross(const struct vec v1, const struct vec v2) {
	struct vec f = {
		v1.y * v2.z - v1.z * v2.y,
		v1.z * v2.x - v1.x * v2.z,
		v1.x * v2.y - v1.y * v2.x,
		v1.w * v2.w
	};
	return f;
}

struct vec mul3(const struct vec v, const double sca) {
	struct vec f = {
		v.x * sca,
		v.y * sca,
		v.z * sca,
		v.w
	};
	return f;
}

double dot3(const struct vec v1, const struct vec v2) {
	return (v1.x * v2.x + v1.y * v2.y + v1.z * v2.z) * v1.w * v2.w;
}

double dot4(const struct vec v1, const struct vec v2) {
	return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z + v1.w * v2.w;
}

struct vec mulMV(const struct mat m, const struct vec v) {
	struct vec f = {
		dot4(m.x, v),
		dot4(m.y, v),
		dot4(m.z, v),
		dot4(m.w, v),
	};
	return f;
}

double lenSq3(const struct vec v) {
	return (v.x * v.x + v.y * v.y + v.z * v.z) / (v.w * v.w);
}

double len3(const struct vec v) {
	return sqrt(lenSq3(v));
}

struct vec normalize3safe(const struct vec v) {
	double len = sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
	if(len < EPS) {
		return VEC_UP;
	}
	struct vec f = mul3(v, 1.0 / len);
	f.w = 1;
	return f;
}

struct vec normalize3homo(const struct vec v) {
	if(fabs(v.w) < EPS) {
		return VEC_ZERO;
	}
	return mul3(v, 1.0 / v.w);
}

struct vec quaternionProduct(const struct vec q1, const struct vec q2) {
	struct vec f = {
		q1.w * q2.x + q1.x * q2.w + q1.y * q2.z - q1.z * q2.y,
		q1.w * q2.y - q1.x * q2.z + q1.y * q2.w + q1.z * q2.x,
		q1.w * q2.z + q1.x * q2.y - q1.y * q2.x + q1.z * q2.w,
		q1.w * q2.w - q1.x * q2.x - q1.y * q2.y - q1.z * q2.z,
	};
	return f;
}

struct vec quaternionConj(const struct vec q) {
	struct vec c = {-q.x, -q.y, -q.z, q.w};
	return c;
}

void writeByte(int fd, uint8_t addr, uint8_t val) {
	uint8_t wr[2] = {addr, val};
	if(write(fd, wr, 2) < 0) {
		perror("Cannot write data");
		_exit(-1);
	}
}

void readBytes(int fd, uint8_t addr, uint8_t *data, uint8_t length) {
	if(write(fd, &addr, 1) < 0) {
		perror("Cannot write address in read operation");
		_exit(-1);
	}
	if(read(fd, data, length) < 0) {
		perror("Cannot read data");
		_exit(-1);
	}
}

uint8_t readByte(int fd, uint8_t addr) {
	uint8_t val;
	readBytes(fd, addr, &val, 1);
	return val;
}

void updateAccSens(int fd, int8_t rel) {
	int8_t val = (int8_t)accSens + rel;
	if(val < 0) {
		val = 0;
	}
	if(val > 3) {
		val = 3;
	}
	if(val != accSens) {
		accSens = val;
		writeByte(fd, 0x1C /* ACCEL_CONFIG */, accSens << 3);
	}
}

void updateGyrSens(int fd, int8_t rel) {
	int8_t val = (int8_t)gyrSens + rel;
	if(val < 0) {
		val = 0;
	}
	if(val > 3) {
		val = 3;
	}
	if(val != gyrSens) {
		gyrSens = val;
		writeByte(fd, 0x1B /* GYRO_CONFIG */, gyrSens << 3);
	}
}

void debug_vec(struct vec v) {
	fprintf(stderr, "%12.6g %12.6g %12.6g %12.6g\n", v.x, v.y, v.z, v.w);
}

void debug_mat(struct mat m) {
	debug_vec(m.x);
	debug_vec(m.y);
	debug_vec(m.z);
	debug_vec(m.w);
}

int loadCalibration(char *filename) {
	int fd;
	if((fd = open(filename, O_RDONLY)) < 0)
		return -1;

	int tr = 0;
	int r;
	while((r = read(fd, &calibOff + tr, sizeof(calibOff) - tr)) > 0)
		tr += r;
	if(r < 0)
		return -1;
	close(fd);

	for(int i = 0; i < 4; ++i) {
		fprintf(stderr, "# accelerometer (%d)\n", i);
		debug_mat(calibOff.accCalib[i]);
		fprintf(stderr, "\n");
	}
	fprintf(stderr, "\n");

	for(int i = 0; i < 4; ++i) {
		fprintf(stderr, "# gyro (%d)\n", i);
		debug_mat(calibOff.gyrCalib[i]);
		fprintf(stderr, "\n");
	}
	fprintf(stderr, "\n");

	fprintf(stderr, "# magnetometer\n");
	debug_mat(calibOff.magCalib);
	fprintf(stderr, "\n");

	return 0;
}

int saveCalibration(char *filename) {
	int fd;
	if((fd = open(filename, O_WRONLY | O_CREAT | O_EXCL, DEFFILEMODE)) < 0)
		return -1;

	int tr = 0;
	int r;
	while((r = write(fd, &calibOff + tr, sizeof(calibOff) - tr)) > 0)
		tr += r;
	if(r < 0)
		return -1;
	close(fd);
	return 0;
}

int main(int argc, char **argv) {

	int flags, opt;

	if(loadCalibration("calib.dat") < 0) {
		perror("Could not load calibration data");
		fprintf(stderr, "Will use identity calibration instead\n");
	}

	/*while((opt = getopt(argc, argv, "ce")) != -1) {
		switch(opt) {
		case 'c':
			isCalib = 1;
			break;
		case 'e':
			fprintf(stderr, "Saving empty calibration file\n");
			_exit(saveCalibration("calib.dat"));
			break;
		default:
			fprintf(stderr, "Usage: %s [-ce]\n", argv[0]);
			_exit(-1);
		}
	}

	if(!isCalib) {
		if(loadCalibration("calib.dat") < 0) {
			_exit(-1);
	}*/

	int agfd;
	int mafd;

//	Open accelerometer + gyro

	if((agfd = open(I2C_DEV, O_RDWR)) < 0) {
		perror("Failed to open " I2C_DEV);
		_exit(-1);
	}

	if(ioctl(agfd, I2C_SLAVE, AG_ADDR) < 0) {
		perror("ioctl failed " STR(AG_ADDR));
		_exit(-1);
	}

//	Init accelerometer + gyro
	writeByte(agfd, 0x6B /* PWR_MGMT_1 */, 0x80);
	usleep(100000);

	writeByte(agfd, 0x37 /* INT_PIN_CFG */, 0x02);

	writeByte(agfd, 0x6B /* PWR_MGMT_1 */, 0x01);
	writeByte(agfd, 0x6C /* PWR_MGMT_2 */, 0x00);
	usleep(200000);
	
	writeByte(agfd, 0x1A /* CONFIG */, 0x00);
	writeByte(agfd, 0x19 /* SMPLRT_DIV */, 0x00);

	writeByte(agfd, 0x1C /* ACCEL_CONFIG */, 0x00);
	writeByte(agfd, 0x1D /* ACCEL_CONFIG2 */, 0x00);
	writeByte(agfd, 0x1B /* GYRO_CONFIG */, 0x00);

	writeByte(agfd, 0x1B /* GYRO_CONFIG */, 0x00);
	writeByte(agfd, 0x1B /* GYRO_CONFIG */, 0x00);
	writeByte(agfd, 0x1B /* GYRO_CONFIG */, 0x00);

	updateAccSens(agfd, 3);
	updateGyrSens(agfd, 3);

//	Open magnetometer
	if((mafd = open(I2C_DEV, O_RDWR)) < 0) {
		perror("Failed to open " I2C_DEV);
		_exit(-1);
	}
	
	if(ioctl(mafd, I2C_SLAVE, MA_ADDR) < 0) {
		perror("ioctl failed " STR(MA_ADDR));
		_exit(-1);
	}
	usleep(100000);

//	Init magnetometer
	writeByte(mafd, 0x0A /* AK_8963_CNTL */, 0x16);
	usleep(100000);

	struct timespec currentTime;
	struct timespec lastTime;
	struct itimerspec timer;
	uint64_t exp;

	int tfd;

	clock_gettime(CLOCK_MONOTONIC, &currentTime);

	timer.it_value = currentTime;
	timer.it_interval.tv_sec = 0;
	timer.it_interval.tv_nsec = 2000000;

	if((tfd = timerfd_create(CLOCK_MONOTONIC, 0)) < 0) {
		perror("Failed to create timerfd");
		_exit(-1);
	}
	if(timerfd_settime(tfd, TFD_TIMER_ABSTIME, &timer, NULL) < 0) {
		perror("Failed to set timerfd time");
		_exit(-1);
	}
	
	uint8_t data[64];

	double agvals[7] = {0};
	double mavals[3] = {0};

	while(1) {

		/*if(read(tfd, &exp, sizeof(exp)) < 0) {
			perror("Failed to read from timerfd");
			_exit(-1);
		}*/

		if(clock_gettime(CLOCK_MONOTONIC, &currentTime) < 0) {
			perror("Failed to get time");
			_exit(-1);
		}

		readBytes(agfd, 0x3B, data, 14);
		for(int i = 0; i < 7; ++i) {
			agvals[i] = ((int16_t)(data[i * 2] << 8 | data[i * 2 + 1]));
		}

		uint8_t magstat = readByte(mafd, 0x02 /* AK8963_ST1 */);
		if(magstat & 1) {
			readBytes(mafd, 0x03, data, 7);
			for(int i = 0; i < 3; ++i) {
				mavals[i] = ((int16_t)(data[i * 2] | data[i * 2 + 1] << 8));
			}
		}

		double temp = agvals[3] / tempSensitivity + 21;

		struct vec acc = mul3((struct vec) {agvals[0], agvals[1], agvals[2], 1}, accScale[accSens]);
		struct vec gyr = mul3((struct vec) {agvals[4], agvals[5], agvals[6], 1}, gyrScale[gyrSens]);
		struct vec mag = mul3((struct vec) {mavals[0], mavals[1], mavals[2], 1}, magScale);

		acc = normalize3homo(mulMV(calibOff.accCalib[accSens], acc));
		gyr = normalize3homo(mulMV(calibOff.gyrCalib[gyrSens], gyr));
		mag = normalize3homo(mulMV(calibOff.magCalib, mag));

		double dt = (currentTime.tv_sec - lastTime.tv_sec + (currentTime.tv_nsec - lastTime.tv_nsec) * 0.000000001);
		lastTime = currentTime;

		// Gyro integration

		double ang = len3(gyr) * 0.5 * dt * TORADIAN;
		struct vec gyq = mul3(normalize3safe(gyr), sin(ang));
		gyq.w = cos(ang);

		rot = quaternionProduct(rot, gyq);
		

		// Accelerometer adjustment (complementary filter)

		struct vec grav = quaternionProduct(quaternionProduct(rot, acc), quaternionConj(rot));
		double gravAdjAng = acos(dot3(normalize3safe(grav), VEC_UP)) * 0.5 * 0.001;
		struct vec gravAdj = mul3(normalize3safe(cross(grav, VEC_UP)), sin(gravAdjAng));
		gravAdj.w = cos(gravAdjAng);

		rot = quaternionProduct(gravAdj, rot);


		printf("%9.6f %9.6f %9.6f %9.6f   %10.6f %10.6f %10.6f   %12.6f %12.6f %12.6f   %10.6f %10.6f %10.6f   %10.3f   %10.3f\n", rot.w, rot.x, rot.y, rot.z, grav.x, grav.y, grav.z, gyr.x, gyr.y, gyr.z, mag.x, mag.y, mag.z, temp, 1.0 / dt);

	}

}
