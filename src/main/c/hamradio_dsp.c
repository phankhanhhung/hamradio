#include "hamradio_dsp.h"

/* ---------- Helper functions ---------- */

static int is_power_of_2(int n) {
    return n > 0 && (n & (n - 1)) == 0;
}

static unsigned int bit_reverse(unsigned int x, int log2n) {
    unsigned int result = 0;
    for (int i = 0; i < log2n; i++) {
        result = (result << 1) | (x & 1);
        x >>= 1;
    }
    return result;
}

static int int_log2(int n) {
    int result = 0;
    while ((1 << result) < n) {
        result++;
    }
    return result;
}

/* ---------- FFT / IFFT ---------- */

/*
 * Internal complex FFT using iterative radix-2 Cooley-Tukey.
 * direction: 1 = forward, -1 = inverse
 * buf: array of complex_f of length 'size', modified in-place.
 */
static int fft_complex(complex_f *buf, int size, int direction) {
    if (!buf) return HR_ERR_NULL_PTR;
    if (!is_power_of_2(size)) return HR_ERR_NOT_POWER_OF_2;

    int log2n = int_log2(size);

    /* Bit-reversal permutation */
    for (int i = 0; i < size; i++) {
        unsigned int j = bit_reverse((unsigned int)i, log2n);
        if (j > (unsigned int)i) {
            complex_f tmp = buf[i];
            buf[i] = buf[j];
            buf[j] = tmp;
        }
    }

    /* Butterfly stages */
    for (int stage = 1; stage <= log2n; stage++) {
        int half_size = 1 << (stage - 1);
        int full_size = 1 << stage;
        float angle_step = (float)(direction) * (float)(-2.0 * M_PI / full_size);

        for (int group = 0; group < size; group += full_size) {
            for (int k = 0; k < half_size; k++) {
                float angle = angle_step * k;
                float wr = cosf(angle);
                float wi = sinf(angle);

                int idx_even = group + k;
                int idx_odd = group + k + half_size;

                complex_f even = buf[idx_even];
                complex_f odd = buf[idx_odd];

                /* Twiddle factor multiplication */
                complex_f tw;
                tw.re = wr * odd.re - wi * odd.im;
                tw.im = wr * odd.im + wi * odd.re;

                buf[idx_even].re = even.re + tw.re;
                buf[idx_even].im = even.im + tw.im;
                buf[idx_odd].re = even.re - tw.re;
                buf[idx_odd].im = even.im - tw.im;
            }
        }
    }

    /* For inverse FFT, scale by 1/N */
    if (direction == -1) {
        float scale = 1.0f / (float)size;
        for (int i = 0; i < size; i++) {
            buf[i].re *= scale;
            buf[i].im *= scale;
        }
    }

    return HR_OK;
}

int fft_forward(const float *input, float *output, int size) {
    if (!input || !output) return HR_ERR_NULL_PTR;
    if (size <= 0) return HR_ERR_INVALID_SIZE;
    if (!is_power_of_2(size)) return HR_ERR_NOT_POWER_OF_2;

    complex_f *buf = (complex_f *)malloc(sizeof(complex_f) * size);
    if (!buf) return HR_ERR_ALLOC;

    /* Copy real input to complex buffer */
    for (int i = 0; i < size; i++) {
        buf[i].re = input[i];
        buf[i].im = 0.0f;
    }

    int ret = fft_complex(buf, size, 1);
    if (ret != HR_OK) {
        free(buf);
        return ret;
    }

    /* Store as interleaved [mag, phase] pairs */
    for (int i = 0; i < size; i++) {
        float mag = sqrtf(buf[i].re * buf[i].re + buf[i].im * buf[i].im);
        float phase = atan2f(buf[i].im, buf[i].re);
        output[2 * i]     = mag;
        output[2 * i + 1] = phase;
    }

    free(buf);
    return HR_OK;
}

int fft_inverse(const float *input, float *output, int size) {
    if (!input || !output) return HR_ERR_NULL_PTR;
    if (size <= 0) return HR_ERR_INVALID_SIZE;
    if (!is_power_of_2(size)) return HR_ERR_NOT_POWER_OF_2;

    complex_f *buf = (complex_f *)malloc(sizeof(complex_f) * size);
    if (!buf) return HR_ERR_ALLOC;

    /* Convert from [mag, phase] pairs to complex */
    for (int i = 0; i < size; i++) {
        float mag   = input[2 * i];
        float phase = input[2 * i + 1];
        buf[i].re = mag * cosf(phase);
        buf[i].im = mag * sinf(phase);
    }

    int ret = fft_complex(buf, size, -1);
    if (ret != HR_OK) {
        free(buf);
        return ret;
    }

    /* Take real part */
    for (int i = 0; i < size; i++) {
        output[i] = buf[i].re;
    }

    free(buf);
    return HR_OK;
}

/* ---------- FIR Filter ---------- */

fir_filter_t* fir_create(const float *coefficients, int num_taps) {
    if (!coefficients || num_taps <= 0) {
        return NULL;
    }

    fir_filter_t *filter = (fir_filter_t *)malloc(sizeof(fir_filter_t));
    if (!filter) return NULL;

    filter->coeffs = (float *)malloc(sizeof(float) * num_taps);
    if (!filter->coeffs) {
        free(filter);
        return NULL;
    }

    filter->delay_line = (float *)calloc(num_taps, sizeof(float));
    if (!filter->delay_line) {
        free(filter->coeffs);
        free(filter);
        return NULL;
    }

    memcpy(filter->coeffs, coefficients, sizeof(float) * num_taps);
    filter->num_taps = num_taps;
    filter->delay_index = 0;

    return filter;
}

int fir_process(fir_filter_t *filter, const float *input, float *output, int num_samples) {
    if (!filter || !input || !output) return HR_ERR_NULL_PTR;
    if (num_samples <= 0) return HR_ERR_INVALID_SIZE;

    int num_taps = filter->num_taps;

    for (int i = 0; i < num_samples; i++) {
        /* Insert new sample into the circular delay line */
        filter->delay_line[filter->delay_index] = input[i];

        float acc = 0.0f;
        int idx = filter->delay_index;

        for (int j = 0; j < num_taps; j++) {
            acc += filter->coeffs[j] * filter->delay_line[idx];
            idx--;
            if (idx < 0) {
                idx = num_taps - 1;
            }
        }

        output[i] = acc;

        filter->delay_index++;
        if (filter->delay_index >= num_taps) {
            filter->delay_index = 0;
        }
    }

    return HR_OK;
}

void fir_destroy(fir_filter_t *filter) {
    if (filter) {
        if (filter->coeffs) free(filter->coeffs);
        if (filter->delay_line) free(filter->delay_line);
        free(filter);
    }
}

/* ---------- IIR Filter (Direct Form II Transposed) ---------- */

iir_filter_t* iir_create(const float *b_coeffs, int num_b, const float *a_coeffs, int num_a) {
    if (!b_coeffs || !a_coeffs || num_b <= 0 || num_a <= 0) {
        return NULL;
    }

    iir_filter_t *filter = (iir_filter_t *)malloc(sizeof(iir_filter_t));
    if (!filter) return NULL;

    filter->b_coeffs = (float *)malloc(sizeof(float) * num_b);
    filter->a_coeffs = (float *)malloc(sizeof(float) * num_a);

    int state_len = (num_b > num_a) ? num_b : num_a;
    filter->state = (float *)calloc(state_len, sizeof(float));

    if (!filter->b_coeffs || !filter->a_coeffs || !filter->state) {
        if (filter->b_coeffs) free(filter->b_coeffs);
        if (filter->a_coeffs) free(filter->a_coeffs);
        if (filter->state) free(filter->state);
        free(filter);
        return NULL;
    }

    memcpy(filter->b_coeffs, b_coeffs, sizeof(float) * num_b);
    memcpy(filter->a_coeffs, a_coeffs, sizeof(float) * num_a);
    filter->num_b = num_b;
    filter->num_a = num_a;

    return filter;
}

int iir_process(iir_filter_t *filter, const float *input, float *output, int num_samples) {
    if (!filter || !input || !output) return HR_ERR_NULL_PTR;
    if (num_samples <= 0) return HR_ERR_INVALID_SIZE;

    int order = (filter->num_b > filter->num_a) ? filter->num_b : filter->num_a;

    /*
     * Direct Form II Transposed:
     *   y[n] = b[0]*x[n] + state[0]
     *   state[k] = b[k+1]*x[n] - a[k+1]*y[n] + state[k+1],  for k = 0..order-2
     *   state[order-1] = b[order-1]*x[n] - a[order-1]*y[n]  (if applicable)
     *
     * Normalize by a[0] if a[0] != 1.
     */
    float a0 = filter->a_coeffs[0];

    for (int i = 0; i < num_samples; i++) {
        float x = input[i];
        float y = (filter->b_coeffs[0] / a0) * x + filter->state[0];
        output[i] = y;

        for (int k = 0; k < order - 1; k++) {
            float b_val = (k + 1 < filter->num_b) ? filter->b_coeffs[k + 1] : 0.0f;
            float a_val = (k + 1 < filter->num_a) ? filter->a_coeffs[k + 1] : 0.0f;
            float next_state = (k + 1 < order - 1) ? filter->state[k + 2] : 0.0f;
            filter->state[k] = (b_val / a0) * x - (a_val / a0) * y + next_state;
        }
    }

    return HR_OK;
}

void iir_destroy(iir_filter_t *filter) {
    if (filter) {
        if (filter->b_coeffs) free(filter->b_coeffs);
        if (filter->a_coeffs) free(filter->a_coeffs);
        if (filter->state) free(filter->state);
        free(filter);
    }
}

/* ---------- Resampler ---------- */

int resample(const float *input, int input_len, int input_rate, int output_rate, float *output, int *output_len) {
    if (!input || !output || !output_len) return HR_ERR_NULL_PTR;
    if (input_len <= 0 || input_rate <= 0 || output_rate <= 0) return HR_ERR_INVALID_SIZE;

    /* Compute output length based on rate ratio */
    double ratio = (double)input_rate / (double)output_rate;
    int out_len = (int)((double)input_len / ratio);
    if (out_len <= 0) {
        *output_len = 0;
        return HR_OK;
    }

    for (int i = 0; i < out_len; i++) {
        double src_index = (double)i * ratio;
        int idx0 = (int)src_index;
        int idx1 = idx0 + 1;
        float frac = (float)(src_index - (double)idx0);

        if (idx1 >= input_len) {
            idx1 = input_len - 1;
        }

        /* Linear interpolation */
        output[i] = input[idx0] * (1.0f - frac) + input[idx1] * frac;
    }

    *output_len = out_len;
    return HR_OK;
}
