#include "hamradio_modem.h"
#include "hamradio_dsp.h"

/* ---------- Internal helpers ---------- */

#define HILBERT_TAPS 63
#define AM_LPF_TAPS  31

/*
 * Generate Hilbert FIR filter coefficients (63 taps).
 * h[n] = 2/(n*PI) for odd (n - center), 0 for even, with Hamming window.
 */
static void generate_hilbert_coeffs(float *coeffs) {
    int center = HILBERT_TAPS / 2;
    for (int i = 0; i < HILBERT_TAPS; i++) {
        int n = i - center;
        if (n == 0) {
            coeffs[i] = 0.0f;
        } else if (n % 2 != 0) {
            /* Odd offset: h[n] = 2 / (n * PI) */
            coeffs[i] = 2.0f / ((float)n * (float)M_PI);
        } else {
            /* Even offset: 0 */
            coeffs[i] = 0.0f;
        }
        /* Apply Hamming window */
        float window = 0.54f - 0.46f * cosf(2.0f * (float)M_PI * (float)i / (float)(HILBERT_TAPS - 1));
        coeffs[i] *= window;
    }
}

/*
 * Generate a simple low-pass FIR filter using windowed sinc.
 * cutoff_ratio = cutoff_freq / sample_rate (normalized, 0 to 0.5)
 */
static void generate_lpf_coeffs(float *coeffs, int num_taps, float cutoff_ratio) {
    int center = num_taps / 2;
    float sum = 0.0f;
    for (int i = 0; i < num_taps; i++) {
        int n = i - center;
        if (n == 0) {
            coeffs[i] = 2.0f * cutoff_ratio;
        } else {
            float x = 2.0f * (float)M_PI * cutoff_ratio * (float)n;
            coeffs[i] = sinf(x) / ((float)M_PI * (float)n);
        }
        /* Hamming window */
        float window = 0.54f - 0.46f * cosf(2.0f * (float)M_PI * (float)i / (float)(num_taps - 1));
        coeffs[i] *= window;
        sum += coeffs[i];
    }
    /* Normalize */
    if (sum != 0.0f) {
        for (int i = 0; i < num_taps; i++) {
            coeffs[i] /= sum;
        }
    }
}

/*
 * Apply an FIR filter directly (stateless, for one-shot use).
 * Output length equals input length; input is zero-padded at the beginning.
 */
static void apply_fir_direct(const float *input, int num_samples, const float *coeffs, int num_taps, float *output) {
    int center = num_taps / 2;
    for (int i = 0; i < num_samples; i++) {
        float acc = 0.0f;
        for (int j = 0; j < num_taps; j++) {
            int idx = i - (j - center);
            if (idx >= 0 && idx < num_samples) {
                acc += coeffs[j] * input[idx];
            }
        }
        output[i] = acc;
    }
}

/* ---------- AM Modulation / Demodulation ---------- */

int modulate_am(const float *baseband, int num_samples, float carrier_freq, int sample_rate, float *output) {
    if (!baseband || !output) return HR_ERR_NULL_PTR;
    if (num_samples <= 0 || sample_rate <= 0) return HR_ERR_INVALID_SIZE;

    float dt = 1.0f / (float)sample_rate;

    for (int i = 0; i < num_samples; i++) {
        float t = (float)i * dt;
        output[i] = (1.0f + baseband[i]) * cosf(2.0f * (float)M_PI * carrier_freq * t);
    }

    return HR_OK;
}

int demodulate_am(const float *signal, int num_samples, float carrier_freq, int sample_rate, float *output) {
    if (!signal || !output) return HR_ERR_NULL_PTR;
    if (num_samples <= 0 || sample_rate <= 0) return HR_ERR_INVALID_SIZE;

    (void)carrier_freq; /* Envelope detection doesn't need carrier freq */

    /* Step 1: absolute value (envelope) */
    float *envelope = (float *)malloc(sizeof(float) * num_samples);
    if (!envelope) return HR_ERR_ALLOC;

    for (int i = 0; i < num_samples; i++) {
        envelope[i] = fabsf(signal[i]);
    }

    /* Step 2: low-pass FIR filter to smooth the envelope */
    float lpf_coeffs[AM_LPF_TAPS];
    /* Cutoff at roughly carrier_freq * 0.1 / sample_rate, but we use a sensible default */
    float cutoff = 0.05f; /* normalized cutoff */
    generate_lpf_coeffs(lpf_coeffs, AM_LPF_TAPS, cutoff);

    float *filtered = (float *)malloc(sizeof(float) * num_samples);
    if (!filtered) {
        free(envelope);
        return HR_ERR_ALLOC;
    }

    apply_fir_direct(envelope, num_samples, lpf_coeffs, AM_LPF_TAPS, filtered);

    /* Step 3: subtract DC (mean value) to recover baseband */
    float dc = 0.0f;
    for (int i = 0; i < num_samples; i++) {
        dc += filtered[i];
    }
    dc /= (float)num_samples;

    for (int i = 0; i < num_samples; i++) {
        output[i] = filtered[i] - dc;
    }

    free(envelope);
    free(filtered);
    return HR_OK;
}

/* ---------- FM Modulation / Demodulation ---------- */

int modulate_fm(const float *baseband, int num_samples, float carrier_freq, float deviation, int sample_rate, float *output) {
    if (!baseband || !output) return HR_ERR_NULL_PTR;
    if (num_samples <= 0 || sample_rate <= 0) return HR_ERR_INVALID_SIZE;

    float dt = 1.0f / (float)sample_rate;
    float integral = 0.0f;

    for (int i = 0; i < num_samples; i++) {
        float t = (float)i * dt;
        integral += baseband[i] * dt;
        output[i] = cosf(2.0f * (float)M_PI * carrier_freq * t + 2.0f * (float)M_PI * deviation * integral);
    }

    return HR_OK;
}

int demodulate_fm(const float *signal, int num_samples, float carrier_freq, float deviation, int sample_rate, float *output) {
    if (!signal || !output) return HR_ERR_NULL_PTR;
    if (num_samples <= 0 || sample_rate <= 0) return HR_ERR_INVALID_SIZE;

    float dt = 1.0f / (float)sample_rate;

    /* Form analytic signal by mixing down with carrier */
    float *i_signal = (float *)malloc(sizeof(float) * num_samples);
    float *q_signal = (float *)malloc(sizeof(float) * num_samples);
    if (!i_signal || !q_signal) {
        if (i_signal) free(i_signal);
        if (q_signal) free(q_signal);
        return HR_ERR_ALLOC;
    }

    /* Mix down to baseband */
    for (int n = 0; n < num_samples; n++) {
        float t = (float)n * dt;
        float phase = 2.0f * (float)M_PI * carrier_freq * t;
        i_signal[n] = signal[n] * cosf(phase);
        q_signal[n] = -signal[n] * sinf(phase);
    }

    /* Low-pass filter both I and Q channels */
    float lpf_coeffs[AM_LPF_TAPS];
    float cutoff = deviation / (float)sample_rate;
    if (cutoff < 0.01f) cutoff = 0.01f;
    if (cutoff > 0.45f) cutoff = 0.45f;
    generate_lpf_coeffs(lpf_coeffs, AM_LPF_TAPS, cutoff);

    float *i_filtered = (float *)malloc(sizeof(float) * num_samples);
    float *q_filtered = (float *)malloc(sizeof(float) * num_samples);
    if (!i_filtered || !q_filtered) {
        free(i_signal);
        free(q_signal);
        if (i_filtered) free(i_filtered);
        if (q_filtered) free(q_filtered);
        return HR_ERR_ALLOC;
    }

    apply_fir_direct(i_signal, num_samples, lpf_coeffs, AM_LPF_TAPS, i_filtered);
    apply_fir_direct(q_signal, num_samples, lpf_coeffs, AM_LPF_TAPS, q_filtered);

    /* Compute instantaneous phase and differentiate */
    float prev_phase = 0.0f;
    for (int n = 0; n < num_samples; n++) {
        float phase = atan2f(q_filtered[n], i_filtered[n]);
        float dphase = phase - prev_phase;

        /* Unwrap phase */
        while (dphase > (float)M_PI) dphase -= 2.0f * (float)M_PI;
        while (dphase < -(float)M_PI) dphase += 2.0f * (float)M_PI;

        /* Convert phase derivative to frequency, then to baseband amplitude */
        output[n] = dphase / (2.0f * (float)M_PI * deviation * dt);

        prev_phase = phase;
    }

    free(i_signal);
    free(q_signal);
    free(i_filtered);
    free(q_filtered);

    return HR_OK;
}

/* ---------- SSB Modulation / Demodulation ---------- */

int modulate_ssb(const float *baseband, int num_samples, float carrier_freq, int sample_rate, int upper_sideband, float *output) {
    if (!baseband || !output) return HR_ERR_NULL_PTR;
    if (num_samples <= 0 || sample_rate <= 0) return HR_ERR_INVALID_SIZE;

    float dt = 1.0f / (float)sample_rate;

    /* Generate Hilbert transform of baseband */
    float hilbert_coeffs[HILBERT_TAPS];
    generate_hilbert_coeffs(hilbert_coeffs);

    float *hilbert_out = (float *)malloc(sizeof(float) * num_samples);
    if (!hilbert_out) return HR_ERR_ALLOC;

    apply_fir_direct(baseband, num_samples, hilbert_coeffs, HILBERT_TAPS, hilbert_out);

    /*
     * Analytic signal: baseband + j * hilbert_out
     * Multiply by carrier: e^(j * 2*PI*fc*t) = cos(wt) + j*sin(wt)
     *
     * For USB: output = Re{ (baseband + j*hilbert) * (cos + j*sin) }
     *        = baseband*cos - hilbert*sin
     *
     * For LSB: negate Q component, so:
     *        = baseband*cos + hilbert*sin
     */
    for (int i = 0; i < num_samples; i++) {
        float t = (float)i * dt;
        float wt = 2.0f * (float)M_PI * carrier_freq * t;
        float cos_wt = cosf(wt);
        float sin_wt = sinf(wt);

        if (upper_sideband) {
            output[i] = baseband[i] * cos_wt - hilbert_out[i] * sin_wt;
        } else {
            output[i] = baseband[i] * cos_wt + hilbert_out[i] * sin_wt;
        }
    }

    free(hilbert_out);
    return HR_OK;
}

int demodulate_ssb(const float *signal, int num_samples, float carrier_freq, int sample_rate, int upper_sideband, float *output) {
    if (!signal || !output) return HR_ERR_NULL_PTR;
    if (num_samples <= 0 || sample_rate <= 0) return HR_ERR_INVALID_SIZE;

    (void)upper_sideband; /* Both USB and LSB demodulate the same way: mix down + LPF */

    float dt = 1.0f / (float)sample_rate;

    /* Multiply by carrier to shift to baseband */
    float *mixed = (float *)malloc(sizeof(float) * num_samples);
    if (!mixed) return HR_ERR_ALLOC;

    for (int i = 0; i < num_samples; i++) {
        float t = (float)i * dt;
        float wt = 2.0f * (float)M_PI * carrier_freq * t;
        mixed[i] = signal[i] * cosf(wt);
    }

    /* Low-pass filter to extract baseband */
    float lpf_coeffs[AM_LPF_TAPS];
    /* Cutoff at about 3kHz for voice SSB */
    float cutoff = 3000.0f / (float)sample_rate;
    if (cutoff > 0.45f) cutoff = 0.45f;
    if (cutoff < 0.01f) cutoff = 0.01f;
    generate_lpf_coeffs(lpf_coeffs, AM_LPF_TAPS, cutoff);

    apply_fir_direct(mixed, num_samples, lpf_coeffs, AM_LPF_TAPS, output);

    /* Scale by 2 to compensate for mixing loss */
    for (int i = 0; i < num_samples; i++) {
        output[i] *= 2.0f;
    }

    free(mixed);
    return HR_OK;
}
