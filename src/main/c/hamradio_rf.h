#ifndef HAMRADIO_RF_H
#define HAMRADIO_RF_H

#include "common.h"

/* Free Space Path Loss in dB */
float compute_fspl(float freq_hz, float distance_m);

/* Apply multipath: sum delayed and attenuated copies of the signal */
int apply_multipath(const float *signal, int num_samples, int num_paths,
                    const float *delays_sec, const float *amplitudes, const float *phase_offsets,
                    int sample_rate, float *output);

/* Apply ionospheric fading: simple sinusoidal amplitude fading model */
int apply_ionospheric_fading(const float *signal, int num_samples,
                              float critical_freq_mhz, float muf_mhz,
                              int sample_rate, float *output);

/* Add AWGN noise floor */
int add_noise_floor(const float *signal, int num_samples, float noise_power_dbm,
                    int sample_rate, float *output);

/* Compute antenna gain (Phase 1: isotropic, returns 0 dB) */
float compute_antenna_gain(float azimuth, float elevation, const float *pattern, int pattern_size);

#endif
