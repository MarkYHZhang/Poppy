#ifndef MONITOR_H
#define MONITOR_H

extern void (*monitor_cb)(uint32_t type, uint32_t len, uint8_t *data);

int monitor_socket(int port);
void monitor_check();
void monitor_msg(char *msg, ...);

#endif
