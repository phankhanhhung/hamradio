#include <jni.h>
#include <stdint.h>
#include "com_hamradio_dsp_NativeDSP.h"
#include "hamradio_dsp.h"
#include "hamradio_modem.h"

/* Global sample rate stored by dspInit */
static int g_sample_rate = 0;

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    dspInit
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_hamradio_dsp_NativeDSP_dspInit
  (JNIEnv *env, jclass cls, jint sampleRate) {
    (void)env;
    (void)cls;
    g_sample_rate = (int)sampleRate;
    return 0;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    dspShutdown
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_hamradio_dsp_NativeDSP_dspShutdown
  (JNIEnv *env, jclass cls) {
    (void)env;
    (void)cls;
    g_sample_rate = 0;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    fftForward
 * Signature: ([FI)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_dsp_NativeDSP_fftForward
  (JNIEnv *env, jclass cls, jfloatArray realSamples, jint size) {
    (void)cls;

    jfloat *input = (*env)->GetFloatArrayElements(env, realSamples, NULL);
    if (!input) return NULL;

    int n = (int)size;
    /* Output is interleaved [mag, phase] pairs, so 2*n floats */
    float *output = (float *)malloc(sizeof(float) * 2 * n);
    if (!output) {
        (*env)->ReleaseFloatArrayElements(env, realSamples, input, JNI_ABORT);
        return NULL;
    }

    int ret = fft_forward(input, output, n);
    (*env)->ReleaseFloatArrayElements(env, realSamples, input, JNI_ABORT);

    if (ret != HR_OK) {
        free(output);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, 2 * n);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, 2 * n, output);
    }
    free(output);
    return result;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    fftInverse
 * Signature: ([FI)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_dsp_NativeDSP_fftInverse
  (JNIEnv *env, jclass cls, jfloatArray freqDomain, jint size) {
    (void)cls;

    jfloat *input = (*env)->GetFloatArrayElements(env, freqDomain, NULL);
    if (!input) return NULL;

    int n = (int)size;
    float *output = (float *)malloc(sizeof(float) * n);
    if (!output) {
        (*env)->ReleaseFloatArrayElements(env, freqDomain, input, JNI_ABORT);
        return NULL;
    }

    int ret = fft_inverse(input, output, n);
    (*env)->ReleaseFloatArrayElements(env, freqDomain, input, JNI_ABORT);

    if (ret != HR_OK) {
        free(output);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, n);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, n, output);
    }
    free(output);
    return result;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    firCreate
 * Signature: ([FI)J
 */
JNIEXPORT jlong JNICALL Java_com_hamradio_dsp_NativeDSP_firCreate
  (JNIEnv *env, jclass cls, jfloatArray coefficients, jint numTaps) {
    (void)cls;

    jfloat *coeffs = (*env)->GetFloatArrayElements(env, coefficients, NULL);
    if (!coeffs) return 0;

    fir_filter_t *filter = fir_create(coeffs, (int)numTaps);
    (*env)->ReleaseFloatArrayElements(env, coefficients, coeffs, JNI_ABORT);

    return (jlong)(intptr_t)filter;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    firProcess
 * Signature: (J[F)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_dsp_NativeDSP_firProcess
  (JNIEnv *env, jclass cls, jlong handle, jfloatArray input) {
    (void)cls;

    fir_filter_t *filter = (fir_filter_t *)(intptr_t)handle;
    if (!filter) return NULL;

    jint len = (*env)->GetArrayLength(env, input);
    jfloat *in_data = (*env)->GetFloatArrayElements(env, input, NULL);
    if (!in_data) return NULL;

    float *out_data = (float *)malloc(sizeof(float) * len);
    if (!out_data) {
        (*env)->ReleaseFloatArrayElements(env, input, in_data, JNI_ABORT);
        return NULL;
    }

    int ret = fir_process(filter, in_data, out_data, (int)len);
    (*env)->ReleaseFloatArrayElements(env, input, in_data, JNI_ABORT);

    if (ret != HR_OK) {
        free(out_data);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, len);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, len, out_data);
    }
    free(out_data);
    return result;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    firDestroy
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_hamradio_dsp_NativeDSP_firDestroy
  (JNIEnv *env, jclass cls, jlong handle) {
    (void)env;
    (void)cls;
    fir_filter_t *filter = (fir_filter_t *)(intptr_t)handle;
    if (filter) {
        fir_destroy(filter);
    }
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    iirCreate
 * Signature: ([F[F)J
 */
JNIEXPORT jlong JNICALL Java_com_hamradio_dsp_NativeDSP_iirCreate
  (JNIEnv *env, jclass cls, jfloatArray bCoeffs, jfloatArray aCoeffs) {
    (void)cls;

    jint num_b = (*env)->GetArrayLength(env, bCoeffs);
    jint num_a = (*env)->GetArrayLength(env, aCoeffs);

    jfloat *b = (*env)->GetFloatArrayElements(env, bCoeffs, NULL);
    jfloat *a = (*env)->GetFloatArrayElements(env, aCoeffs, NULL);
    if (!b || !a) {
        if (b) (*env)->ReleaseFloatArrayElements(env, bCoeffs, b, JNI_ABORT);
        if (a) (*env)->ReleaseFloatArrayElements(env, aCoeffs, a, JNI_ABORT);
        return 0;
    }

    iir_filter_t *filter = iir_create(b, (int)num_b, a, (int)num_a);

    (*env)->ReleaseFloatArrayElements(env, bCoeffs, b, JNI_ABORT);
    (*env)->ReleaseFloatArrayElements(env, aCoeffs, a, JNI_ABORT);

    return (jlong)(intptr_t)filter;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    iirProcess
 * Signature: (J[F)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_dsp_NativeDSP_iirProcess
  (JNIEnv *env, jclass cls, jlong handle, jfloatArray input) {
    (void)cls;

    iir_filter_t *filter = (iir_filter_t *)(intptr_t)handle;
    if (!filter) return NULL;

    jint len = (*env)->GetArrayLength(env, input);
    jfloat *in_data = (*env)->GetFloatArrayElements(env, input, NULL);
    if (!in_data) return NULL;

    float *out_data = (float *)malloc(sizeof(float) * len);
    if (!out_data) {
        (*env)->ReleaseFloatArrayElements(env, input, in_data, JNI_ABORT);
        return NULL;
    }

    int ret = iir_process(filter, in_data, out_data, (int)len);
    (*env)->ReleaseFloatArrayElements(env, input, in_data, JNI_ABORT);

    if (ret != HR_OK) {
        free(out_data);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, len);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, len, out_data);
    }
    free(out_data);
    return result;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    iirDestroy
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_hamradio_dsp_NativeDSP_iirDestroy
  (JNIEnv *env, jclass cls, jlong handle) {
    (void)env;
    (void)cls;
    iir_filter_t *filter = (iir_filter_t *)(intptr_t)handle;
    if (filter) {
        iir_destroy(filter);
    }
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    modulateAM
 * Signature: ([FFI)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_dsp_NativeDSP_modulateAM
  (JNIEnv *env, jclass cls, jfloatArray baseband, jfloat carrierFreq, jint sampleRate) {
    (void)cls;

    jint len = (*env)->GetArrayLength(env, baseband);
    jfloat *in_data = (*env)->GetFloatArrayElements(env, baseband, NULL);
    if (!in_data) return NULL;

    float *out_data = (float *)malloc(sizeof(float) * len);
    if (!out_data) {
        (*env)->ReleaseFloatArrayElements(env, baseband, in_data, JNI_ABORT);
        return NULL;
    }

    int ret = modulate_am(in_data, (int)len, carrierFreq, (int)sampleRate, out_data);
    (*env)->ReleaseFloatArrayElements(env, baseband, in_data, JNI_ABORT);

    if (ret != HR_OK) {
        free(out_data);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, len);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, len, out_data);
    }
    free(out_data);
    return result;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    demodulateAM
 * Signature: ([FFI)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_dsp_NativeDSP_demodulateAM
  (JNIEnv *env, jclass cls, jfloatArray signal, jfloat carrierFreq, jint sampleRate) {
    (void)cls;

    jint len = (*env)->GetArrayLength(env, signal);
    jfloat *in_data = (*env)->GetFloatArrayElements(env, signal, NULL);
    if (!in_data) return NULL;

    float *out_data = (float *)malloc(sizeof(float) * len);
    if (!out_data) {
        (*env)->ReleaseFloatArrayElements(env, signal, in_data, JNI_ABORT);
        return NULL;
    }

    int ret = demodulate_am(in_data, (int)len, carrierFreq, (int)sampleRate, out_data);
    (*env)->ReleaseFloatArrayElements(env, signal, in_data, JNI_ABORT);

    if (ret != HR_OK) {
        free(out_data);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, len);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, len, out_data);
    }
    free(out_data);
    return result;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    modulateFM
 * Signature: ([FFFI)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_dsp_NativeDSP_modulateFM
  (JNIEnv *env, jclass cls, jfloatArray baseband, jfloat carrierFreq, jfloat deviation, jint sampleRate) {
    (void)cls;

    jint len = (*env)->GetArrayLength(env, baseband);
    jfloat *in_data = (*env)->GetFloatArrayElements(env, baseband, NULL);
    if (!in_data) return NULL;

    float *out_data = (float *)malloc(sizeof(float) * len);
    if (!out_data) {
        (*env)->ReleaseFloatArrayElements(env, baseband, in_data, JNI_ABORT);
        return NULL;
    }

    int ret = modulate_fm(in_data, (int)len, carrierFreq, deviation, (int)sampleRate, out_data);
    (*env)->ReleaseFloatArrayElements(env, baseband, in_data, JNI_ABORT);

    if (ret != HR_OK) {
        free(out_data);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, len);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, len, out_data);
    }
    free(out_data);
    return result;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    demodulateFM
 * Signature: ([FFFI)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_dsp_NativeDSP_demodulateFM
  (JNIEnv *env, jclass cls, jfloatArray signal, jfloat carrierFreq, jfloat deviation, jint sampleRate) {
    (void)cls;

    jint len = (*env)->GetArrayLength(env, signal);
    jfloat *in_data = (*env)->GetFloatArrayElements(env, signal, NULL);
    if (!in_data) return NULL;

    float *out_data = (float *)malloc(sizeof(float) * len);
    if (!out_data) {
        (*env)->ReleaseFloatArrayElements(env, signal, in_data, JNI_ABORT);
        return NULL;
    }

    int ret = demodulate_fm(in_data, (int)len, carrierFreq, deviation, (int)sampleRate, out_data);
    (*env)->ReleaseFloatArrayElements(env, signal, in_data, JNI_ABORT);

    if (ret != HR_OK) {
        free(out_data);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, len);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, len, out_data);
    }
    free(out_data);
    return result;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    modulateSSB
 * Signature: ([FFIZ)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_dsp_NativeDSP_modulateSSB
  (JNIEnv *env, jclass cls, jfloatArray baseband, jfloat carrierFreq, jint sampleRate, jboolean upperSideband) {
    (void)cls;

    jint len = (*env)->GetArrayLength(env, baseband);
    jfloat *in_data = (*env)->GetFloatArrayElements(env, baseband, NULL);
    if (!in_data) return NULL;

    float *out_data = (float *)malloc(sizeof(float) * len);
    if (!out_data) {
        (*env)->ReleaseFloatArrayElements(env, baseband, in_data, JNI_ABORT);
        return NULL;
    }

    int usb = upperSideband ? 1 : 0;
    int ret = modulate_ssb(in_data, (int)len, carrierFreq, (int)sampleRate, usb, out_data);
    (*env)->ReleaseFloatArrayElements(env, baseband, in_data, JNI_ABORT);

    if (ret != HR_OK) {
        free(out_data);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, len);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, len, out_data);
    }
    free(out_data);
    return result;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    demodulateSSB
 * Signature: ([FFIZ)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_dsp_NativeDSP_demodulateSSB
  (JNIEnv *env, jclass cls, jfloatArray signal, jfloat carrierFreq, jint sampleRate, jboolean upperSideband) {
    (void)cls;

    jint len = (*env)->GetArrayLength(env, signal);
    jfloat *in_data = (*env)->GetFloatArrayElements(env, signal, NULL);
    if (!in_data) return NULL;

    float *out_data = (float *)malloc(sizeof(float) * len);
    if (!out_data) {
        (*env)->ReleaseFloatArrayElements(env, signal, in_data, JNI_ABORT);
        return NULL;
    }

    int usb = upperSideband ? 1 : 0;
    int ret = demodulate_ssb(in_data, (int)len, carrierFreq, (int)sampleRate, usb, out_data);
    (*env)->ReleaseFloatArrayElements(env, signal, in_data, JNI_ABORT);

    if (ret != HR_OK) {
        free(out_data);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, len);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, len, out_data);
    }
    free(out_data);
    return result;
}

/*
 * Class:     com_hamradio_dsp_NativeDSP
 * Method:    resample
 * Signature: ([FII)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_hamradio_dsp_NativeDSP_resample
  (JNIEnv *env, jclass cls, jfloatArray input, jint inputRate, jint outputRate) {
    (void)cls;

    jint input_len = (*env)->GetArrayLength(env, input);
    jfloat *in_data = (*env)->GetFloatArrayElements(env, input, NULL);
    if (!in_data) return NULL;

    /* Estimate maximum output length */
    int max_out_len = (int)((double)input_len * (double)outputRate / (double)inputRate) + 2;
    float *out_data = (float *)malloc(sizeof(float) * max_out_len);
    if (!out_data) {
        (*env)->ReleaseFloatArrayElements(env, input, in_data, JNI_ABORT);
        return NULL;
    }

    int actual_out_len = 0;
    int ret = resample(in_data, (int)input_len, (int)inputRate, (int)outputRate, out_data, &actual_out_len);
    (*env)->ReleaseFloatArrayElements(env, input, in_data, JNI_ABORT);

    if (ret != HR_OK) {
        free(out_data);
        return NULL;
    }

    jfloatArray result = (*env)->NewFloatArray(env, actual_out_len);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, actual_out_len, out_data);
    }
    free(out_data);
    return result;
}
