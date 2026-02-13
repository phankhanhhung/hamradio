#ifndef HAMRADIO_DSP_H
#define HAMRADIO_DSP_H

#include "common.h"

/* FFT / IFFT */
int fft_forward(const float *input, float *output, int size);
int fft_inverse(const float *input, float *output, int size);

/* FIR Filter */
typedef struct {
    float *coeffs;
    float *delay_line;
    int num_taps;
    int delay_index;
} fir_filter_t;

fir_filter_t* fir_create(const float *coefficients, int num_taps);
int fir_process(fir_filter_t *filter, const float *input, float *output, int num_samples);
void fir_destroy(fir_filter_t *filter);

/* IIR Filter */
typedef struct {
    float *b_coeffs;   /* numerator */
    float *a_coeffs;   /* denominator */
    int num_b;
    int num_a;
    float *state;       /* filter state (max of num_b, num_a) */
} iir_filter_t;

iir_filter_t* iir_create(const float *b_coeffs, int num_b, const float *a_coeffs, int num_a);
int iir_process(iir_filter_t *filter, const float *input, float *output, int num_samples);
void iir_destroy(iir_filter_t *filter);

/* Resampler */
int resample(const float *input, int input_len, int input_rate, int output_rate, float *output, int *output_len);

#endif
