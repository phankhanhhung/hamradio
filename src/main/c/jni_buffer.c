#include <jni.h>
#include <stdint.h>
#include "com_hamradio_dsp_buffer_NativeBuffer.h"
#include "hamradio_buffer.h"

/*
 * Class:     com_hamradio_dsp_buffer_NativeBuffer
 * Method:    bufferCreate
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_hamradio_dsp_buffer_NativeBuffer_bufferCreate
  (JNIEnv *env, jclass cls, jint capacitySamples) {
    (void)env;
    (void)cls;
    ring_buffer_t *rb = ring_buffer_create((int)capacitySamples);
    return (jlong)(intptr_t)rb;
}

/*
 * Class:     com_hamradio_dsp_buffer_NativeBuffer
 * Method:    bufferWrite
 * Signature: (J[FII)I
 */
JNIEXPORT jint JNICALL Java_com_hamradio_dsp_buffer_NativeBuffer_bufferWrite
  (JNIEnv *env, jclass cls, jlong handle, jfloatArray data, jint offset, jint length) {
    (void)cls;

    ring_buffer_t *rb = (ring_buffer_t *)(intptr_t)handle;
    if (!rb) return HR_ERR_NULL_PTR;

    jint arr_len = (*env)->GetArrayLength(env, data);
    jfloat *arr = (*env)->GetFloatArrayElements(env, data, NULL);
    if (!arr) return HR_ERR_NULL_PTR;

    if (offset < 0 || length < 0 || offset + length > arr_len) {
        (*env)->ReleaseFloatArrayElements(env, data, arr, JNI_ABORT);
        return HR_ERR_INVALID_SIZE;
    }

    int written = ring_buffer_write(rb, arr + offset, (int)length);

    (*env)->ReleaseFloatArrayElements(env, data, arr, JNI_ABORT);

    return (jint)written;
}

/*
 * Class:     com_hamradio_dsp_buffer_NativeBuffer
 * Method:    bufferRead
 * Signature: (J[FII)I
 */
JNIEXPORT jint JNICALL Java_com_hamradio_dsp_buffer_NativeBuffer_bufferRead
  (JNIEnv *env, jclass cls, jlong handle, jfloatArray dest, jint offset, jint maxLength) {
    (void)cls;

    ring_buffer_t *rb = (ring_buffer_t *)(intptr_t)handle;
    if (!rb) return HR_ERR_NULL_PTR;

    jint arr_len = (*env)->GetArrayLength(env, dest);
    jfloat *arr = (*env)->GetFloatArrayElements(env, dest, NULL);
    if (!arr) return HR_ERR_NULL_PTR;

    if (offset < 0 || maxLength < 0 || offset + maxLength > arr_len) {
        (*env)->ReleaseFloatArrayElements(env, dest, arr, JNI_ABORT);
        return HR_ERR_INVALID_SIZE;
    }

    int read_count = ring_buffer_read(rb, arr + offset, (int)maxLength);

    /* Commit changes back to the Java array (mode 0 = copy back and free) */
    (*env)->ReleaseFloatArrayElements(env, dest, arr, 0);

    return (jint)read_count;
}

/*
 * Class:     com_hamradio_dsp_buffer_NativeBuffer
 * Method:    bufferAvailable
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_hamradio_dsp_buffer_NativeBuffer_bufferAvailable
  (JNIEnv *env, jclass cls, jlong handle) {
    (void)env;
    (void)cls;
    ring_buffer_t *rb = (ring_buffer_t *)(intptr_t)handle;
    if (!rb) return HR_ERR_NULL_PTR;
    return (jint)ring_buffer_available(rb);
}

/*
 * Class:     com_hamradio_dsp_buffer_NativeBuffer
 * Method:    bufferReset
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_hamradio_dsp_buffer_NativeBuffer_bufferReset
  (JNIEnv *env, jclass cls, jlong handle) {
    (void)env;
    (void)cls;
    ring_buffer_t *rb = (ring_buffer_t *)(intptr_t)handle;
    if (rb) {
        ring_buffer_reset(rb);
    }
}

/*
 * Class:     com_hamradio_dsp_buffer_NativeBuffer
 * Method:    bufferDestroy
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_hamradio_dsp_buffer_NativeBuffer_bufferDestroy
  (JNIEnv *env, jclass cls, jlong handle) {
    (void)env;
    (void)cls;
    ring_buffer_t *rb = (ring_buffer_t *)(intptr_t)handle;
    if (rb) {
        ring_buffer_destroy(rb);
    }
}
