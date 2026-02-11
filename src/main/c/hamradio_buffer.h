#ifndef HAMRADIO_BUFFER_H
#define HAMRADIO_BUFFER_H

#include "common.h"

typedef struct {
    float *data;
    int capacity;
    int read_pos;   /* only modified by consumer (reader) */
    int write_pos;  /* only modified by producer (writer) */
} ring_buffer_t;

ring_buffer_t* ring_buffer_create(int capacity);
void ring_buffer_destroy(ring_buffer_t *rb);
int ring_buffer_write(ring_buffer_t *rb, const float *data, int count);
int ring_buffer_read(ring_buffer_t *rb, float *dest, int count);
int ring_buffer_available(const ring_buffer_t *rb);
void ring_buffer_reset(ring_buffer_t *rb);

#endif
