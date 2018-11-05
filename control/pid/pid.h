#ifndef PID_H
#define PID_H

#include <time.h>

struct pid {
	double kp;
	double ki;
	double kd;

	double vi;
	double vd;

	double pmin;
	double pmax;
	double imin, iimin;
	double imax, iimax;
	double dmin;
	double dmax;

	double last;
};

struct timespec get_time();
double get_dt(struct timespec *last);
double get_timediff(const struct timespec a, const struct timespec b);

double pid_update(struct pid *p, double feedback, double target, double dt);
struct pid pid_simple(double kp, double ki, double kd);

#endif
