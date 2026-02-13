#ifndef HAMRADIO_MODEM_H
#define HAMRADIO_MODEM_H

#include "common.h"

/* AM */
int modulate_am(const float *baseband, int num_samples, float carrier_freq, int sample_rate, float *output);
int demodulate_am(const float *signal, int num_samples, float carrier_freq, int sample_rate, float *output);

/* FM */
int modulate_fm(const float *baseband, int num_samples, float carrier_freq, float deviation, int sample_rate, float *output);
int demodulate_fm(const float *signal, int num_samples, float carrier_freq, float deviation, int sample_rate, float *output);

/* SSB (upper_sideband: 1=USB, 0=LSB) */
int modulate_ssb(const float *baseband, int num_samples, float carrier_freq, int sample_rate, int upper_sideband, float *output);
int demodulate_ssb(const float *signal, int num_samples, float carrier_freq, int sample_rate, int upper_sideband, float *output);

#endif
