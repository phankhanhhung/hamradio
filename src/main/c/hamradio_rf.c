#include "hamradio_rf.h"
#include <time.h>

/* ---------- Free Space Path Loss ---------- */

float compute_fspl(float freq_hz, float distance_m) {
    if (freq_hz <= 0.0f || distance_m <= 0.0f) {
        return 0.0f;
    }
    /* FSPL = 20*log10(d) + 20*log10(f) - 147.55 */
    return 20.0f * log10f(distance_m) + 20.0f * log10f(freq_hz) - 147.55f;
}

/* ---------- Multipath ---------- */

int apply_multipath(const float *signal, int num_samples, int num_paths,
                    const float *delays_sec, const float *amplitudes, const float *phase_offsets,
                    int sample_rate, float *output) {
    if (!signal || !delays_sec || !amplitudes || !phase_offsets || !output) {
        return HR_ERR_NULL_PTR;
    }
    if (num_samples <= 0 || num_paths <= 0 || sample_rate <= 0) {
        return HR_ERR_INVALID_SIZE;
    }

    /* Initialize output to zero */
    memset(output, 0, sizeof(float) * num_samples);

    for (int p = 0; p < num_paths; p++) {
        int delay_samples = (int)(delays_sec[p] * (float)sample_rate + 0.5f);
        float amp = amplitudes[p];
        float phase = phase_offsets[p];

        for (int i = 0; i < num_samples; i++) {
            int src_idx = i - delay_samples;
            if (src_idx >= 0 && src_idx < num_samples) {
                /*
                 * Apply phase offset as a rotation: for a real signal,
                 * phase offset effectively scales the delayed copy.
                 * Using: delayed_sample * amplitude * cos(phase_offset)
                 * (The phase_offset models the carrier phase shift.)
                 */
                output[i] += signal[src_idx] * amp * cosf(phase);
            }
        }
    }

    return HR_OK;
}

/* ---------- Ionospheric Fading ---------- */

int apply_ionospheric_fading(const float *signal, int num_samples,
                              float critical_freq_mhz, float muf_mhz,
                              int sample_rate, float *output) {
    if (!signal || !output) return HR_ERR_NULL_PTR;
    if (num_samples <= 0 || sample_rate <= 0) return HR_ERR_INVALID_SIZE;

    /*
     * Simple ionospheric fading model:
     *
     * - If the operating frequency is above MUF: heavy attenuation (skip zone).
     *   Signal passes through the ionosphere instead of reflecting.
     *
     * - If the operating frequency is below the critical frequency:
     *   Strong absorption, attenuate.
     *
     * - In the usable range (critical_freq < f < MUF):
     *   Apply slow sinusoidal fading at 0.1 - 1 Hz.
     *
     * Since we don't know the actual operating frequency from the signal,
     * we use the MUF and critical frequency to determine a fading factor.
     */

    /* Compute a fading rate in the range 0.1 to 1.0 Hz */
    float fading_rate_hz = 0.5f; /* default fading rate */

    /* If MUF is too close to critical frequency, the usable band is narrow and fading is stronger */
    float bandwidth_ratio = 1.0f;
    if (muf_mhz > critical_freq_mhz && critical_freq_mhz > 0.0f) {
        bandwidth_ratio = (muf_mhz - critical_freq_mhz) / muf_mhz;
        /* Narrower usable band = faster fading */
        fading_rate_hz = 0.1f + 0.9f * (1.0f - bandwidth_ratio);
    }

    float dt = 1.0f / (float)sample_rate;

    /* Determine base attenuation factor based on frequency relationship */
    float base_atten;
    if (muf_mhz <= 0.0f || critical_freq_mhz <= 0.0f) {
        /* Invalid parameters, pass signal through with mild fading */
        base_atten = 0.9f;
    } else if (critical_freq_mhz >= muf_mhz) {
        /* Unusual condition: heavy attenuation */
        base_atten = 0.1f;
    } else {
        /* Normal propagation: mild fading */
        base_atten = 0.7f + 0.3f * bandwidth_ratio;
    }

    for (int i = 0; i < num_samples; i++) {
        float t = (float)i * dt;
        /* Sinusoidal fading envelope */
        float fading = base_atten * (0.8f + 0.2f * sinf(2.0f * (float)M_PI * fading_rate_hz * t));
        output[i] = signal[i] * fading;
    }

    return HR_OK;
}

/* ---------- AWGN Noise Floor ---------- */

/* Box-Muller transform: generate a pair of Gaussian random numbers */
static float box_muller_gauss(void) {
    static int has_spare = 0;
    static float spare;

    if (has_spare) {
        has_spare = 0;
        return spare;
    }

    float u, v, s;
    do {
        u = ((float)rand() / (float)RAND_MAX) * 2.0f - 1.0f;
        v = ((float)rand() / (float)RAND_MAX) * 2.0f - 1.0f;
        s = u * u + v * v;
    } while (s >= 1.0f || s == 0.0f);

    float factor = sqrtf(-2.0f * logf(s) / s);
    spare = v * factor;
    has_spare = 1;
    return u * factor;
}

int add_noise_floor(const float *signal, int num_samples, float noise_power_dbm,
                    int sample_rate, float *output) {
    if (!signal || !output) return HR_ERR_NULL_PTR;
    if (num_samples <= 0 || sample_rate <= 0) return HR_ERR_INVALID_SIZE;

    (void)sample_rate; /* Not needed for simple noise addition */

    /* Seed the random number generator (only once) */
    static int seeded = 0;
    if (!seeded) {
        srand((unsigned int)time(NULL));
        seeded = 1;
    }

    /*
     * Convert dBm to linear power (in watts, 50 ohm reference):
     *   P_linear = 10^(dBm / 10) / 1000
     * The noise amplitude (std dev) is sqrt(P_linear).
     * We treat the signal as voltage, so noise std dev = sqrt(P).
     */
    float p_linear = powf(10.0f, noise_power_dbm / 10.0f) / 1000.0f;
    float noise_std = sqrtf(p_linear);

    for (int i = 0; i < num_samples; i++) {
        float noise = box_muller_gauss() * noise_std;
        output[i] = signal[i] + noise;
    }

    return HR_OK;
}

/* ---------- Antenna Gain ---------- */

float compute_antenna_gain(float azimuth, float elevation, const float *pattern, int pattern_size) {
    /* Phase 1: isotropic antenna, returns 0 dBi */
    (void)azimuth;
    (void)elevation;
    (void)pattern;
    (void)pattern_size;
    return 0.0f;
}
