#include <time.h>
#include "pid.h"

#define EPS 1e-9

struct timespec get_time() {
	struct timespec curr;
	clock_gettime(CLOCK_MONOTONIC, &curr); 
	return curr;
}

double get_dt(struct timespec *last) {
	struct timespec curr = get_time();
	double dt = curr.tv_sec - last->tv_sec + (curr.tv_nsec - last->tv_nsec) * 1e-9;
	*last = curr;
	return dt;
}

double get_timediff(const struct timespec a, const struct timespec b) {
	return a.tv_sec - b.tv_sec + (a.tv_nsec - b.tv_nsec) * 1e-9;
}

double clamp(double x, double min, double max) {
	return x < min ? min : (x > max ? max : x);
}

double pid_update(struct pid *pid_dat, double feedback, double target, double dt) {
	double err = feedback - target;

	double p = clamp(err * pid_dat->kp, pid_dat->pmin, pid_dat->pmax);

	pid_dat->vi = clamp(pid_dat->vi + (err * pid_dat->ki) * dt, pid_dat->iimin, pid_dat->iimax);

	double i = clamp(pid_dat->vi * pid_dat->ki, pid_dat->imin, pid_dat->imax);

	if(dt < EPS)
		return p + i;

	double d = clamp((err - pid_dat->last) / dt * pid_dat->kd, pid_dat->dmin, pid_dat->dmax);
	pid_dat->last = err;

	return p + i + d;
}

struct pid pid_simple(double kp, double ki, double kd) {
	struct pid p = {0};
	p.kp = kp;
	p.ki = ki;
	p.kd = kd;

	p.pmin = p.imin = p.iimin = p.dmin = -1.0 / 0.0;
	p.pmax = p.imax = p.iimax = p.dmax = 1.0 / 0.0;
	return p;
}
