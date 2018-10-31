#ifndef MONITOR_H
#define MONITOR_H

int monitor_socket(int port);
void monitor_check();
void monitor_msg(char *msg, ...);

#endif
