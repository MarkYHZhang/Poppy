#include <unistd.h>
#include <fcntl.h>
#include <stdint.h>
#include <math.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>
#include <linux/spi/spidev.h>
#include <sys/stat.h>
#include <string.h>
#include <stdio.h>

#include "vec.h"
#include "imu.h"
#include "MadgwickAHRS.h"
#include "../monitor/monitor.h"

#define I2C_DEV "/dev/i2c-1"
#define AG_ADDR 0x68
#define MA_ADDR 0x0C

#define STRIFY(x) #x
#define STR(x) STRIFY(x)

// g / lsb
double acc_lsb[4] = {   2.0 / 32768.0,    4.0 / 32768.0,    8.0 / 32768.0,   16.0 / 32768.0};
// degrees / lsb
double gyr_lsb[4] = { 250.0 / 32768.0,  500.0 / 32768.0, 1000.0 / 32768.0, 2000.0 / 32768.0};
// mT / lsb
double mag_lsb[1] = {0.15 * 1e-3};

uint8_t acc_sens = 0;
uint8_t gyr_sens = 0;
uint8_t mag_sens = 0;

double temp_lsb = 1.0 / 333.87;
double temp_off = 21.0;

struct calib_data {
	struct mat4 acc_calib;
	struct mat4 gyr_calib;
	struct mat4 mag_calib;
};

struct calib_data calib_off = {MAT4_IDENTITY, MAT4_IDENTITY, MAT4_IDENTITY};
	
uint8_t data[64];

double agvals[7];
double mavals[3];

int agfd;
int mafd;

double imu_acc_weight = 0.001;
double imu_mag_weight = 0.001;

double imu_temp;
struct vec3 imu_acc;
struct vec3 imu_gyr;
struct vec3 imu_mag;
struct vec4 imu_rot = QUAT_IDENTITY;

void write_byte(int fd, uint8_t addr, uint8_t val) {
	uint8_t wr[2] = {addr, val};
	if(write(fd, wr, 2) < 0) {
		perror("Cannot write data");
		_exit(-1);
	}
}

void read_bytes(int fd, uint8_t addr, uint8_t *data, uint8_t length) {
	if(write(fd, &addr, 1) < 0) {
		perror("Cannot write address in read operation");
		_exit(-1);
	}
	if(read(fd, data, length) < 0) {
		perror("Cannot read data");
		_exit(-1);
	}
}

uint8_t read_byte(int fd, uint8_t addr) {
	uint8_t val;
	read_bytes(fd, addr, &val, 1);
	return val;
}

void imu_set_acc_sens(uint8_t sens) {
	acc_sens = sens;
	write_byte(agfd, 0x1C /* ACCEL_CONFIG */, acc_sens << 3);
}

void imu_set_gyr_sens(uint8_t sens) {
	gyr_sens = sens;
	write_byte(agfd, 0x1B /* GYRO_CONFIG */, gyr_sens << 3);
}

void debug_vec4(struct vec4 v) {
	fprintf(stderr, "%12.6g %12.6g %12.6g %12.6g\n", v.x, v.y, v.z, v.w);
}

void debug_mat4(struct mat4 m) {
	debug_vec4(m.x);
	debug_vec4(m.y);
	debug_vec4(m.z);
	debug_vec4(m.w);
}

int imu_load_calib(char *filename) {
	int fd;
	if((fd = open(filename, O_RDONLY)) < 0)
		return -1;

	int tr = 0;
	int r;
	while((r = read(fd, &calib_off + tr, sizeof(struct calib_data) - tr)) > 0)
		tr += r;
	if(r < 0)
		return -1;
	close(fd);

	fprintf(stderr, "# accelerometer\n");
	debug_mat4(calib_off.acc_calib);
	fprintf(stderr, "\n");

	fprintf(stderr, "# gyro\n");
	debug_mat4(calib_off.gyr_calib);
	fprintf(stderr, "\n");

	fprintf(stderr, "# magnetometer\n");
	debug_mat4(calib_off.mag_calib);
	fprintf(stderr, "\n");

	return 0;
}

int imu_save_calib(char *filename) {
	int fd;
	if((fd = open(filename, O_WRONLY | O_CREAT | O_EXCL, DEFFILEMODE)) < 0)
		return -1;

	int tr = 0;
	int r;
	while((r = write(fd, &calib_off + tr, sizeof(struct calib_data) - tr)) > 0)
		tr += r;
	if(r < 0)
		return -1;
	close(fd);
	return 0;
}

void monitor_mat4(const char *str, struct mat4 m) {
	monitor_msg("%s %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f\n", str, m.x.x, m.x.y, m.x.z, m.x.w, m.y.x, m.y.y, m.y.z, m.y.w, m.z.x, m.z.y, m.z.z, m.z.w, m.w.x, m.w.y, m.w.z, m.w.w);
}

void imu_monitor_get_calib() {
	monitor_mat4("imu calib a", calib_off.acc_calib);
	monitor_mat4("imu calib g", calib_off.gyr_calib);
	monitor_mat4("imu calib m", calib_off.mag_calib);
}

void imu_monitor_set_calib(uint32_t len, uint8_t *data) {
	if(len != sizeof(struct calib_data)) {
		fprintf(stderr, "error, received calibration data has incorrect size");
	}
	calib_off = *(struct calib_data *) data;
}

void imu_init() {

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
	write_byte(agfd, 0x6B /* PWR_MGMT_1 */, 0x80);
	usleep(100000);

	write_byte(agfd, 0x37 /* INT_PIN_CFG */, 0x02);

	write_byte(agfd, 0x6B /* PWR_MGMT_1 */, 0x01);
	write_byte(agfd, 0x6C /* PWR_MGMT_2 */, 0x00);
	usleep(200000);
	
	write_byte(agfd, 0x1A /* CONFIG */, 0x00);
	write_byte(agfd, 0x19 /* SMPLRT_DIV */, 0x00);

	write_byte(agfd, 0x1C /* ACCEL_CONFIG */, 0x00);
	write_byte(agfd, 0x1D /* ACCEL_CONFIG2 */, 0x00);
	write_byte(agfd, 0x1B /* GYRO_CONFIG */, 0x00);

	write_byte(agfd, 0x1B /* GYRO_CONFIG */, 0x00);
	write_byte(agfd, 0x1B /* GYRO_CONFIG */, 0x00);
	write_byte(agfd, 0x1B /* GYRO_CONFIG */, 0x00);

	imu_set_acc_sens(3);
	imu_set_gyr_sens(3);

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
	write_byte(mafd, 0x0A /* AK_8963_CNTL */, 0x16);
	usleep(100000);
}

void imu_update(double dt, uint32_t calib_iteration) {

	read_bytes(agfd, 0x3B, data, 14);
	for(int i = 0; i < 7; ++i) {
		agvals[i] = ((int16_t)(data[i * 2] << 8 | data[i * 2 + 1]));
	}

	uint8_t magstat = read_byte(mafd, 0x02 /* AK8963_ST1 */);
	if(magstat & 1) {
		read_bytes(mafd, 0x03, data, 7);
		for(int i = 0; i < 3; ++i) {
			mavals[i] = ((int16_t)(data[i * 2] | data[i * 2 + 1] << 8));
		}
	}

	imu_temp = agvals[3] * temp_lsb + temp_off;

	imu_acc = norm4_h(mul4_mv(calib_off.acc_calib, (struct vec4) {agvals[0], agvals[1], agvals[2], 1.0 / acc_lsb[acc_sens]}));
	imu_gyr = norm4_h(mul4_mv(calib_off.gyr_calib, (struct vec4) {agvals[4], agvals[5], agvals[6], 1.0 / gyr_lsb[gyr_sens]}));
	imu_mag = norm4_h(mul4_mv(calib_off.mag_calib, (struct vec4) {mavals[1], mavals[0],-mavals[2], 1.0 / mag_lsb[mag_sens]}));



	if(calib_iteration == 0) {
		MadgwickAHRSupdate(imu_gyr.x / 180 * M_PI, imu_gyr.y / 180 * M_PI, imu_gyr.z / 180 * M_PI, imu_acc.x, imu_acc.y, imu_acc.z, imu_mag.x, imu_mag.y, imu_mag.z);

		imu_rot = (struct vec4) {q1, q2, q3, q0};

		monitor_msg("imu dat %9.6f %9.6f %9.6f %9.6f   %10.6f %10.6f %10.6f   %12.6f %12.6f %12.6f   %10.6f %10.6f %10.6f   %10.3f\n", imu_rot.w, imu_rot.x, imu_rot.y, imu_rot.z, imu_acc.x, imu_acc.y, imu_acc.z, imu_gyr.x, imu_gyr.y, imu_gyr.z, imu_mag.x, imu_mag.y, imu_mag.z, imu_temp);
	} else {
		for(uint32_t i = 0; i < calib_iteration; ++i) {
			// Accelerometer adjustment (complementary filter)
			struct vec3 grav = quat_rot(imu_rot, imu_acc);
			double grav_adj_ang = ang3(grav, VEC3_ZP) * imu_acc_weight;
			struct vec4 grav_adj = quat_from_aa(cross3(grav, VEC3_ZP), grav_adj_ang);
			imu_rot = quat_prod(grav_adj, imu_rot);

			// Magnetometer adjustment (complementary filter)
			struct vec3 mag_field = quat_rot(imu_rot, imu_mag);
			mag_field.z = 0;
			double mag_adj_ang = ang3(mag_field, VEC3_XP) * imu_mag_weight;
			struct vec4 mag_adj = quat_from_aa(cross3(mag_field, VEC3_XP), mag_adj_ang);
			imu_rot = quat_prod(mag_adj, imu_rot);
		}
		q0 = imu_rot.w;
		q1 = imu_rot.x;
		q2 = imu_rot.y;
		q3 = imu_rot.z;
	}


}
