CC=gcc

all: pi-blaster

pi-blaster: mailbox.c pi-blaster.c
	$(CC) -o $@ $^

clean:
	rm -f pi-blaster
