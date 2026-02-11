#include "hamradio_buffer.h"

/*
 * Lock-free SPSC (Single Producer, Single Consumer) ring buffer.
 * Uses GCC __atomic built-ins for memory ordering between producer and consumer threads.
 * - Producer only writes write_pos (after writing data)
 * - Consumer only writes read_pos (after reading data)
 * - Both can read the other's position with acquire semantics
 */

ring_buffer_t* ring_buffer_create(int capacity) {
    if (capacity <= 0) {
        return NULL;
    }

    ring_buffer_t *rb = (ring_buffer_t *)malloc(sizeof(ring_buffer_t));
    if (!rb) {
        return NULL;
    }

    /* Allocate one extra slot to distinguish full from empty */
    rb->data = (float *)malloc(sizeof(float) * (capacity + 1));
    if (!rb->data) {
        free(rb);
        return NULL;
    }

    rb->capacity = capacity + 1;
    rb->read_pos = 0;
    rb->write_pos = 0;

    return rb;
}

void ring_buffer_destroy(ring_buffer_t *rb) {
    if (rb) {
        if (rb->data) {
            free(rb->data);
        }
        free(rb);
    }
}

int ring_buffer_write(ring_buffer_t *rb, const float *data, int count) {
    if (!rb || !data) {
        return HR_ERR_NULL_PTR;
    }
    if (count <= 0) {
        return 0;
    }

    int wp = rb->write_pos;
    int rp = __atomic_load_n(&rb->read_pos, __ATOMIC_ACQUIRE);
    int cap = rb->capacity;

    /* Available space for writing */
    int available_space;
    if (wp >= rp) {
        available_space = cap - 1 - (wp - rp);
    } else {
        available_space = rp - wp - 1;
    }

    int to_write = (count < available_space) ? count : available_space;

    for (int i = 0; i < to_write; i++) {
        rb->data[wp] = data[i];
        wp = (wp + 1) % cap;
    }

    /* Release: ensure data writes are visible before updating write_pos */
    __atomic_store_n(&rb->write_pos, wp, __ATOMIC_RELEASE);

    return to_write;
}

int ring_buffer_read(ring_buffer_t *rb, float *dest, int count) {
    if (!rb || !dest) {
        return HR_ERR_NULL_PTR;
    }
    if (count <= 0) {
        return 0;
    }

    int rp = rb->read_pos;
    int wp = __atomic_load_n(&rb->write_pos, __ATOMIC_ACQUIRE);
    int cap = rb->capacity;

    /* Available data for reading */
    int available_data;
    if (wp >= rp) {
        available_data = wp - rp;
    } else {
        available_data = cap - rp + wp;
    }

    int to_read = (count < available_data) ? count : available_data;

    for (int i = 0; i < to_read; i++) {
        dest[i] = rb->data[rp];
        rp = (rp + 1) % cap;
    }

    /* Release: ensure data reads complete before updating read_pos */
    __atomic_store_n(&rb->read_pos, rp, __ATOMIC_RELEASE);

    return to_read;
}

int ring_buffer_available(const ring_buffer_t *rb) {
    if (!rb) {
        return HR_ERR_NULL_PTR;
    }

    int rp = __atomic_load_n(&((ring_buffer_t *)rb)->read_pos, __ATOMIC_ACQUIRE);
    int wp = __atomic_load_n(&((ring_buffer_t *)rb)->write_pos, __ATOMIC_ACQUIRE);
    int cap = rb->capacity;

    if (wp >= rp) {
        return wp - rp;
    } else {
        return cap - rp + wp;
    }
}

void ring_buffer_reset(ring_buffer_t *rb) {
    if (rb) {
        __atomic_store_n(&rb->read_pos, 0, __ATOMIC_RELEASE);
        __atomic_store_n(&rb->write_pos, 0, __ATOMIC_RELEASE);
    }
}
