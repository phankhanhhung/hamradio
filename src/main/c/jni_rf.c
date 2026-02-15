#include <jni.h>
#include <stdint.h>
#include "com_hamradio_rf_NativeRF.h"
#include "hamradio_rf.h"

/*
 * Class:     com_hamradio_rf_NativeRF
 * Method:    computeFSPL
 * Signature: (FF)F
 */
JNIEXPORT jfloat JNICALL Java_com_hamradio_rf_NativeRF_computeFSPL
  (JNIEnv *env, jclass cls, jfloat frequencyHz, jfloat distanceMeters) {
    (void)env;
    (void)cls;
    return (jfloat)compute_fspl(frequencyHz, distanceMeters);
}

/*
 * Class:     com_hamradio_rf_NativeRF
 * Method:    applyMultipath
 * Signature: ([FI[F[F[FI)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_rf_NativeRF_applyMultipath
  (JNIEnv *env, jclass cls, jfloatArray signal, jint numPaths,
   jfloatArray delaysSec, jfloatArray amplitudes, jfloatArray phaseOffsets,
   jint sampleRate) {
    (void)cls;

    jint num_samples = (*env)->GetArrayLength(env, signal);

    jfloat *sig_data = (*env)->GetFloatArrayElements(env, signal, NULL);
    jfloat *delays_data = (*env)->GetFloatArrayElements(env, delaysSec, NULL);
    jfloat *amp_data = (*env)->GetFloatArrayElements(env, amplitudes, NULL);
    jfloat *phase_data = (*env)->GetFloatArrayElements(env, phaseOffsets, NULL);

    if (!sig_data || !delays_data || !amp_data || !phase_data) {
        if (sig_data) (*env)->ReleaseFloatArrayElements(env, signal, sig_data, JNI_ABORT);
        if (delays_data) (*env)->ReleaseFloatArrayElements(env, delaysSec, delays_data, JNI_ABORT);
        if (amp_data) (*env)->ReleaseFloatArrayElements(env, amplitudes, amp_data, JNI_ABORT);
        if (phase_data) (*env)->ReleaseFloatArrayElements(env, phaseOffsets, phase_data, JNI_ABORT);
        return NULL;
    }

    float *out_data = (float *)malloc(sizeof(float) * num_samples);
    if (!out_data) {
        (*env)->ReleaseFloatArrayElements(env, signal, sig_data, JNI_ABORT);
        (*env)->ReleaseFloatArrayElements(env, delaysSec, delays_data, JNI_ABORT);
        (*env)->ReleaseFloatArrayElements(env, amplitudes, amp_data, JNI_ABORT);
        (*env)->ReleaseFloatArrayElements(env, phaseOffsets, phase_data, JNI_ABORT);
        return NULL;
    }

    int ret = apply_multipath(sig_data, (int)num_samples, (int)numPaths,
                              delays_data, amp_data, phase_data,
                              (int)sampleRate, out_data);

    (*env)->ReleaseFloatArrayElements(env, signal, sig_data, JNI_ABORT);
    (*env)->ReleaseFloatArrayElements(env, delaysSec, delays_data, JNI_ABORT);
    (*env)->ReleaseFloatArrayElements(env, amplitudes, amp_data, JNI_ABORT);
    (*env)->ReleaseFloatArrayElements(env, phaseOffsets, phase_data, JNI_ABORT);

    if (ret != HR_OK) {
        free(out_data);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, num_samples);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, num_samples, out_data);
    }
    free(out_data);
    return result;
}

/*
 * Class:     com_hamradio_rf_NativeRF
 * Method:    applyIonosphericFading
 * Signature: ([FFFI)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_rf_NativeRF_applyIonosphericFading
  (JNIEnv *env, jclass cls, jfloatArray signal, jfloat criticalFreqMHz,
   jfloat maxUsableFreqMHz, jint sampleRate) {
    (void)cls;

    jint num_samples = (*env)->GetArrayLength(env, signal);
    jfloat *sig_data = (*env)->GetFloatArrayElements(env, signal, NULL);
    if (!sig_data) return NULL;

    float *out_data = (float *)malloc(sizeof(float) * num_samples);
    if (!out_data) {
        (*env)->ReleaseFloatArrayElements(env, signal, sig_data, JNI_ABORT);
        return NULL;
    }

    int ret = apply_ionospheric_fading(sig_data, (int)num_samples,
                                        criticalFreqMHz, maxUsableFreqMHz,
                                        (int)sampleRate, out_data);

    (*env)->ReleaseFloatArrayElements(env, signal, sig_data, JNI_ABORT);

    if (ret != HR_OK) {
        free(out_data);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, num_samples);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, num_samples, out_data);
    }
    free(out_data);
    return result;
}

/*
 * Class:     com_hamradio_rf_NativeRF
 * Method:    addNoiseFloor
 * Signature: ([FFI)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_rf_NativeRF_addNoiseFloor
  (JNIEnv *env, jclass cls, jfloatArray signal, jfloat noisePowerDbm, jint sampleRate) {
    (void)cls;

    jint num_samples = (*env)->GetArrayLength(env, signal);
    jfloat *sig_data = (*env)->GetFloatArrayElements(env, signal, NULL);
    if (!sig_data) return NULL;

    float *out_data = (float *)malloc(sizeof(float) * num_samples);
    if (!out_data) {
        (*env)->ReleaseFloatArrayElements(env, signal, sig_data, JNI_ABORT);
        return NULL;
    }

    int ret = add_noise_floor(sig_data, (int)num_samples, noisePowerDbm,
                               (int)sampleRate, out_data);

    (*env)->ReleaseFloatArrayElements(env, signal, sig_data, JNI_ABORT);

    if (ret != HR_OK) {
        free(out_data);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, num_samples);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, num_samples, out_data);
    }
    free(out_data);
    return result;
}

/*
 * Class:     com_hamradio_rf_NativeRF
 * Method:    computeAntennaGain
 * Signature: (FF[F)F
 */
JNIEXPORT jfloat JNICALL Java_com_hamradio_rf_NativeRF_computeAntennaGain
  (JNIEnv *env, jclass cls, jfloat azimuth, jfloat elevation, jfloatArray pattern) {
    (void)cls;

    jfloat *pat_data = NULL;
    jint pat_size = 0;

    if (pattern) {
        pat_size = (*env)->GetArrayLength(env, pattern);
        pat_data = (*env)->GetFloatArrayElements(env, pattern, NULL);
    }

    float gain = compute_antenna_gain(azimuth, elevation, pat_data, (int)pat_size);

    if (pat_data) {
        (*env)->ReleaseFloatArrayElements(env, pattern, pat_data, JNI_ABORT);
    }

    return (jfloat)gain;
}
