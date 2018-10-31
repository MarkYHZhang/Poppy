#include <stdio.h>
#include <unistd.h>

#include "pi-blaster/pi-blaster.h"
#include "imu/imu.h"
#include "imu/vec.h"
#include "monitor/monitor.h"

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
	motor_set(0, forward + turn);
	motor_set(1, -(forward - turn));
	pwm_update();
}

int main() {
	monitor_socket(8080);

	uint8_t pins[] = {12, 13, 19, 26};
	pwm_init(pins, 4, 0);

	if(imu_load_calib("imu/calib.dat") < 0) {
		perror("Could not load calibration data");
	}
	imu_init();

	double output_lowpass = 0;

	while(1) {
		monitor_check();
		imu_update();
		struct vec3 rx = quat_rot(imu_rot, VEC3_XP);
		struct vec3 rz = quat_rot(imu_rot, VEC3_ZP);
		double ang = atan2(rx.z, rz.z) / M_PI * 180;
		if(ang != ang)
			goto stall;

		ang -= 9.6;

		printf("%f\n", ang);

		if(fabs(ang) > 45)
			goto stall;

		double output = -ang / 10;

		move(output, 0);

		continue;
stall:
		move(0, 0);
	}

}
