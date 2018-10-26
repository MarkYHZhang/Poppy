CC=gcc

LIBS=-lm

all: imu calib_gen

calib_gen: calib_gen.c
	$(CC) -o $@ $^

calib: calib_gen calib.txt
	./calib_gen < calib.txt > calib.dat

imu: imu.c
	$(CC) -o $@ $< $(LIBS)

clean:
	rm -f imu calib_gen
