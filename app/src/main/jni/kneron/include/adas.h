#ifndef DASHCAMDEMO_ADAS_H
#define DASHCAMDEMO_ADAS_H



#ifdef _MSC_VER
	typedef __int32 int32_t;
	typedef unsigned __int32 uint32_t;
	typedef __int64 int64_t;
	typedef unsigned __int64 uint64_t;
	typedef unsigned char uint8_t;
	typedef signed char int8_t;
#else
	#include <stdint.h>
#endif

typedef struct _tagCarDistance {
    int x;
    int y;
    int width;
    int height;
    int distance;    
    
} CarDistance;


int adas_car_detect(uint8_t *frame, CarDistance *distance, unsigned int *count);

//  释放资源
void adas_free();

int adas_init(int width, int height);


#endif //DASHCAMDEMO_ADAS_H
