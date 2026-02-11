#ifndef HAMRADIO_COMMON_H
#define HAMRADIO_COMMON_H

#include <math.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#define HR_OK 0
#define HR_ERR_NULL_PTR -1
#define HR_ERR_INVALID_SIZE -2
#define HR_ERR_ALLOC -3
#define HR_ERR_NOT_POWER_OF_2 -4

typedef struct {
    float re;
    float im;
} complex_f;

#endif
