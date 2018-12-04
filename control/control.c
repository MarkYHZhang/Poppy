#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/timerfd.h>
#include <poll.h>
#include <string.h>

#include "pi-blaster/pi-blaster.h"
#include "imu/imu.h"
#include "imu/vec.h"
#include "monitor/monitor.h"
#include "pid/pid.h"

void motor_set(uint8_t motor, double speed) {
	if(speed >= 0) {
		pwm[motor * 2] = speed * 2000;
		pwm[motor * 2 + 1] = 0;
	} else {
		pwm[motor * 2] = 0;
		pwm[motor * 2 + 1] = -speed * 2000;
	}
}

void move(double forward, double turn) {
	if(turn > 0.5) 
		turn = 0.5;
	if(turn < -0.5)
		turn = -0.5;
	double m1 = forward + turn;
	double m2 = forward - turn;
	motor_set(0, m1);
	motor_set(1, m2);
	monitor_msg("out %f %f\n", m1, m2);
	pwm_update();
}

double tilt_offset = 134.5;
double heading_set = 0;
double velocity_set = 0;

void on_monitor_data(uint32_t type, uint32_t len, uint8_t *data) {
	switch(type) {
		case 1:
			imu_monitor_get_calib();
			break;
		case 2:
			imu_monitor_set_calib(len, data);
			break;
		case 3:
			imu_save_calib("imu/calib.dat");
			break;
		case 4:
			tilt_offset = *(double *) data;
			break;
		case 5:
			heading_set = *(double *) data;
			break;
		case 6:
			velocity_set = *(double *) data;
			break;
	}
}

int main() {
	monitor_socket(8080);
	monitor_cb = on_monitor_data;

	int cfd = open("/tmp/poppypipe", O_RDONLY);
	if(cfd < 0) {
		perror("open\n");
		return 1;
	}
	struct pollfd pfd;
	pfd.fd = cfd;
	pfd.events = POLLIN;

	FILE *in = fdopen(cfd, "r");

	uint8_t pins[] = {12, 13, 19, 26};
	pwm_init(pins, 4, 0);

	if(imu_load_calib("imu/calib.dat") < 0) {
		perror("Could not load calibration data");
	}
	imu_init();

	double output_lowpass = 0;

	imu_acc_weight = 0.1;
	imu_mag_weight = 0.1;
	imu_update(0, 100);
//	imu_acc_weight = 0.005;
//	imu_mag_weight = 0.002;

	struct timespec t = get_time();

	struct pid pid_tilt = pid_simple(0.1, 0.6, 0.0005);
	struct pid pid_speed = pid_simple(0.5, 0.15, 0.00);
	struct pid pid_turn = pid_simple(0.6, 0.0, 0.000);

//	pid_tilt.iimax = 10;
//	pid_tilt.iimin = -10;

//	pid_speed.iimax = 0.5;
//	pid_speed.iimin = -0.5;

	pid_turn.iimax = 0.1;
	pid_turn.iimin = -0.1;

	int tfd = timerfd_create(CLOCK_MONOTONIC, 0);
	struct itimerspec timer = {};
	timer.it_interval.tv_nsec = 1000000000 / 500;
	timer.it_value.tv_nsec = 1000000000 / 500;
	timerfd_settime(tfd, 0, &timer, NULL);

	double dt;
	double vvss = 0;

	for(;; monitor_msg("dt %f %f\n", dt, get_timediff(get_time(), t))) {
		uint64_t missed;
		read(tfd, &missed, sizeof(missed));

		dt = get_dt(&t);

		imu_update(dt, 0);

		monitor_check();

		if(poll(&pfd, 1, 0) > 0) {
			char c[1024];
			double d;
			fscanf(in, "%s %lf", c, &d);
			if(strncmp(c, "move", 4) == 0) {
				printf("move: %f\n", d);
				pid_speed.vi -= d / 10;
			} else if(strncmp(c, "turn", 4) == 0) {
				printf("turn: %f\n", d);
				heading_set += d;
			}
		}

		struct vec3 rx = quat_rot(imu_rot, VEC3_XP);
		struct vec3 ry = quat_rot(imu_rot, VEC3_YP);
		struct vec3 rz = quat_rot(imu_rot, VEC3_ZP);
		double tilt = atan2(rx.z, rz.z) / M_PI * 180 + tilt_offset;
		double heading = atan2(rx.x, ry.x) / M_PI * 180;
		if(tilt != tilt || heading != heading)
			goto stall;

		monitor_msg("ori %f %f\n", tilt, heading);

		vvss += (velocity_set - vvss) / 300;

		if(fabs(tilt) > 30) {
			pid_tilt.vi = 0;
			pid_speed.vi = 0;
			goto stall;
		}

		double tilt_set = atan(pid_update(&pid_speed, vvss * 0.4, output_lowpass, dt)) / M_PI * 180;
		if(tilt_set < -20) {
			tilt_set = -20;
		} else if(tilt_set > 20) {
			tilt_set = 20;
		}
		double output = tan(pid_update(&pid_tilt, tilt, tilt_set, dt) / 180.0 * M_PI) * 100;
		if(output < -1)
			output = -1;
		if(output > 1)
			output = 1;
		output_lowpass += (output - output_lowpass) / 400;
		printf("%12.6f\n", tilt);
//		printf("%12.6f %12.6f %12.6f\n", output, output_lowpass, tilt_set);
	//	printf("Output: %12f %12f %12f\n", tilt, tilt - tilt_set, output_lowpass);
	
		double heading_diff = (heading + heading_set) / 180 * M_PI;
		heading_diff = atan2(cos(heading_diff), sin(heading_diff));

		double turn = pid_update(&pid_turn, heading, heading + heading_diff, dt);
//
//		printf("Turn: %f\n", turn);

		move(output, turn);

		continue;
stall:;
		move(0, 0);
	}

}
