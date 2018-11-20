#include <stdint.h>
#include "vec.h"

double clamp1(double x) {
	return x < -1 ? -1 : (x > 1 ? 1 : x);
}

struct vec4 to_vec4(const struct vec3 v, const double w) {
	const struct vec4 f = {
		v.x,
		v.y,
		v.z,
		w
	};
	return f;
}

struct vec3 to_vec3(const struct vec4 v) {
	const struct vec3 f = {
		v.x,
		v.y,
		v.z,
	};
	return f;
}

double dot3(const struct vec3 v1, const struct vec3 v2) {
	return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
}

struct vec3 cross3(const struct vec3 v1, const struct vec3 v2) {
	const struct vec3 f = {
		v1.y * v2.z - v1.z * v2.y,
		v1.z * v2.x - v1.x * v2.z,
		v1.x * v2.y - v1.y * v2.x,
	};
	return f;
}

struct vec3 mul3_s(const struct vec3 v, const double d) {
	const struct vec3 f = {
		v.x * d,
		v.y * d,
		v.z * d,
	};
	return f;
}

double len3_sq(const struct vec3 v) {
	return v.x * v.x + v.y * v.y + v.z * v.z;
}

double len3(const struct vec3 v) {
	return sqrt(len3_sq(v));
}

struct vec3 norm3(const struct vec3 v) {
	return mul3_s(v, 1.0 / len3(v));
}

double ang3(const struct vec3 v1, const struct vec3 v2) {
	double len1 = len3(v1);
	double len2 = len3(v2);
	if(len1 < EPS || len2 < EPS) {
		return 0;
	}
	return acos(clamp1(dot3(mul3_s(v1, 1.0 / len1), mul3_s(v2, 1.0 / len2))));
}

double dot4(const struct vec4 v1, const struct vec4 v2) {
	return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z + v1.w * v2.w;
}

struct vec4 mul4_mv(const struct mat4 m, const struct vec4 v) {
	const struct vec4 f = {
		dot4(m.x, v),
		dot4(m.y, v),
		dot4(m.z, v),
		dot4(m.w, v),
	};
	return f;
}

struct vec3 norm4_h(const struct vec4 v) {
	if(fabs(v.w) < EPS) {
		return VEC3_ZERO;
	}
	return mul3_s(to_vec3(v), 1.0 / v.w);
}

struct vec4 quat_prod(const struct vec4 q1, const struct vec4 q2) {
	const struct vec4 f = {
		q1.w * q2.x + q1.x * q2.w + q1.y * q2.z - q1.z * q2.y,
		q1.w * q2.y - q1.x * q2.z + q1.y * q2.w + q1.z * q2.x,
		q1.w * q2.z + q1.x * q2.y - q1.y * q2.x + q1.z * q2.w,
		q1.w * q2.w - q1.x * q2.x - q1.y * q2.y - q1.z * q2.z,
	};
	return f;
}

struct vec4 quat_conj(const struct vec4 q) {
	return (struct vec4) {-q.x, -q.y, -q.z, q.w};
}

struct vec3 quat_rot(const struct vec4 q, const struct vec3 v) {
	return to_vec3(quat_prod(quat_prod(q, to_vec4(v, 0)), quat_conj(q)));
}

struct vec4 quat_from_aa(const struct vec3 axis, double ang) {
	double len = len3(axis);
	if(len < EPS) {
		return QUAT_IDENTITY;
	}
	ang /= 2.0;
	return to_vec4(mul3_s(mul3_s(axis, 1.0 / len), sin(ang)), cos(ang));
}

