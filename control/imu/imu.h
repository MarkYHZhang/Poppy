#ifndef IMU_H
#define IMU_H

#include <stdint.h>
#include "vec.h"

extern double imu_temp;
extern struct vec3 imu_acc;
extern struct vec3 imu_gyr;
extern struct vec3 imu_mag;
extern struct vec4 imu_rot;

int imu_load_calib(char *filename);
int imu_save_calib(char *filename);

void imu_init();
void imu_update();

#endif
