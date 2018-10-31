#ifndef VEC_H
#define VEC_H

#include <stdint.h>
#include <math.h>

#define TORADIAN M_PI / 180.0;
#define EPS 1e-9

struct vec4 {
	double x;
	double y;
	double z;
	double w;
};

struct vec3 {
	double x;
	double y;
	double z;
};

struct mat4 {
	struct vec4 x;
	struct vec4 y;
	struct vec4 z;
	struct vec4 w;
};

#define VEC3_XM ((struct vec3) {-1,  0,  0})
#define VEC3_XP ((struct vec3) { 1,  0,  0})
#define VEC3_YM ((struct vec3) { 0, -1,  0})
#define VEC3_YP ((struct vec3) { 0,  1,  0})
#define VEC3_ZM ((struct vec3) { 0,  0, -1})
#define VEC3_ZP ((struct vec3) { 0,  0,  1})

#define VEC3_ZERO ((struct vec3) {0, 0, 0})
#define QUAT_IDENTITY ((struct vec4) {0, 0, 0, 1})

#define MAT4_IDENTITY ((struct mat4) {{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}})

double clamp1(double x);
struct vec4 to_vec4(const struct vec3 v, const double w);
struct vec3 to_vec3(const struct vec4 v);
double dot3(const struct vec3 v1, const struct vec3 v2);
struct vec3 cross3(const struct vec3 v1, const struct vec3 v2);
struct vec3 mul3_s(const struct vec3 v, const double d);
double len3_sq(const struct vec3 v);
double len3(const struct vec3 v);
struct vec3 norm3(const struct vec3 v);
double ang3(const struct vec3 v1, const struct vec3 v2);
double dot4(const struct vec4 v1, const struct vec4 v2);
struct vec4 mul4_mv(const struct mat4 m, const struct vec4 v);
struct vec3 norm4_h(const struct vec4 v);
struct vec4 quat_prod(const struct vec4 q1, const struct vec4 q2);
struct vec4 quat_conj(const struct vec4 q);
struct vec3 quat_rot(const struct vec4 q, const struct vec3 v);
struct vec4 quat_from_aa(const struct vec3 axis, double ang);

#endif
