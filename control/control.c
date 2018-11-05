#include <stdio.h>
#include <unistd.h>
#include <sys/timerfd.h>

#include "pi-blaster/pi-blaster.h"
#include "imu/imu.h"
#include "imu/vec.h"
#include "monitor/monitor.h"
#include "pid/pid.h"

void motor_set(uint8_t motor, double speed) {
	if(speed >= 0) {
		pwm[motor * 2] = speed * 1000;
		pwm[motor * 2 + 1] = 0;
	} else {
		pwm[motor * 2] = 0;
		pwm[motor * 2 + 1] = -speed * 1000;
	}
}

void move(double forward, double turn) {
	double m1 = forward + turn;
	double m2 = forward - turn;
	motor_set(0, m1);
	motor_set(1, -m2);
	monitor_msg("out %f %f\n", m1, m2);
	pwm_update();
}

double tilt_offset = 9.6;
double heading_set = 0;

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
	}
}

int main() {
	monitor_socket(8080);
	monitor_cb = on_monitor_data;

	uint8_t pins[] = {12, 13, 19, 26};
	pwm_init(pins, 4, 0);

	if(imu_load_calib("imu/calib.dat") < 0) {
		perror("Could not load calibration data");
	}
	imu_init();

	double output_lowpass = 0;

	imu_acc_weight = 0.25;
	imu_mag_weight = 0.25;
	for(int i = 0; i < 100; ++i) {
		imu_update(0);
	}
	imu_acc_weight = 0.001;
	imu_mag_weight = 0.001;

	struct timespec t = get_time();

	struct pid pid_tilt = pid_simple(0.1, 0.5, 0.002);
	struct pid pid_speed = pid_simple(0.1, 0.5, 0.002);
	struct pid pid_turn = pid_simple(0.5, 0.5, 0.002);

	pid_speed.iimax = 0.1;
	pid_speed.iimin = -0.1;

	pid_turn.iimax = 0.1;
	pid_turn.iimin = -0.1;

	int tfd = timerfd_create(CLOCK_MONOTONIC, 0);
	struct itimerspec timer = {};
	timer.it_interval.tv_nsec = 1000000000 / 250;
	timer.it_value.tv_nsec = 1000000000 / 250;
	timerfd_settime(tfd, 0, &timer, NULL);

	double dt;

	for(;; monitor_msg("dt %f %f\n", dt, get_timediff(get_time(), t))) {
		uint64_t missed;
		read(tfd, &missed, sizeof(missed));

		dt = get_dt(&t);

		imu_update(dt);

		monitor_check();

		struct vec3 rx = quat_rot(imu_rot, VEC3_XP);
		struct vec3 ry = quat_rot(imu_rot, VEC3_YP);
		struct vec3 rz = quat_rot(imu_rot, VEC3_ZP);
		double tilt = atan2(rx.z, rz.z) / M_PI * 180;
		double heading = atan2(rx.x, ry.x) / M_PI * 180;
		if(tilt != tilt || heading != heading)
			goto stall;

		monitor_msg("ori %f %f\n", tilt, heading);

		if(fabs(tilt) > 45)
			goto stall;

		double tilt_set = tilt_offset + 100 * pid_update(&pid_speed, output_lowpass, 0, dt);
		if(tilt_set < -20) {
			tilt_set = -20;
		} else if(tilt_set > 20) {
			tilt_set = 20;
		}
		double output = -pid_update(&pid_tilt, tilt, tilt_set, dt);
		if(output < -1)
			output = -1;
		if(output > 1)
			output = 1;
		output_lowpass += (output - output_lowpass) / 50;
	//	printf("Output: %12f %12f %12f\n", tilt, tilt - tilt_set, output_lowpass);
	
		double heading_diff = (heading - heading_set) / 180 * M_PI;
		heading_diff = atan2(cos(heading_diff), sin(heading_diff));

		double turn = -pid_update(&pid_turn, heading, heading + heading_diff, dt);

		printf("Turn: %f\n", turn);

		move(output, turn);

		continue;
stall:;
		move(0, 0);
	}

}
