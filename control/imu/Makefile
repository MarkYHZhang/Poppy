CC=gcc

LIBS=-lm

all: imu calib_gen matmul

calib_gen: calib_gen.c
	$(CC) -o $@ $^

calib: calib_gen calib.txt
	./calib_gen < calib.txt > calib.dat

matmul: matmul.c
	$(CC) -o $@ $^

imu: imu.c
	$(CC) -o $@ $^ $(LIBS)

clean:
	rm -f imu calib_gen
