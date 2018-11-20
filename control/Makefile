CC=gcc -g
LIBS=-lm

all: control

pi-blaster/pi-blaster.o: pi-blaster/pi-blaster.c
	$(CC) -o $@ $^ -c

pi-blaster/mailbox.o: pi-blaster/mailbox.c
	$(CC) -o $@ $^ -c

imu/imu.o: imu/imu.c
	$(CC) -o $@ $^ -c

imu/vec.o: imu/vec.c
	$(CC) -o $@ $^ -c

monitor/monitor.o: monitor/monitor.c
	$(CC) -o $@ $^ -c

pid/pid.o: pid/pid.c
	$(CC) -o $@ $^ -c

control: control.c pi-blaster/pi-blaster.o pi-blaster/mailbox.o imu/imu.o imu/vec.o monitor/monitor.o pid/pid.o
	$(CC) -o $@ $^ $(LIBS)

clean:
	rm -f imu/*.o pi-blaster/*.o monitor/*.o control imu/calib_gen imu/matmul
