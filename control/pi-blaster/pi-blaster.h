#ifndef PI_BLASTER_H
#define PI_BLASTER_H

#include <stdint.h>

#define MAX_CHANNELS 32

extern uint32_t pwm[MAX_CHANNELS];

void pwm_init(uint8_t *channel_pins, uint8_t nchannels, int use_pcm);
void pwm_update();

#endif
