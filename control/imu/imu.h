#ifndef IMU_H
#define IMU_H

#include <stdint.h>
#include "vec.h"

extern double imu_temp;
extern struct vec3 imu_acc;
extern struct vec3 imu_gyr;
extern struct vec3 imu_mag;
extern struct vec4 imu_rot;

extern double imu_acc_weight;
extern double imu_mag_weight;

int imu_load_calib(char *filename);
int imu_save_calib(char *filename);
void imu_monitor_get_calib();
void imu_monitor_set_calib(uint32_t len, uint8_t *data);
void imu_set_acc_sens(uint8_t sens);
void imu_set_gyr_sens(uint8_t sens);

void imu_init();
void imu_update(double dt);

#endif
