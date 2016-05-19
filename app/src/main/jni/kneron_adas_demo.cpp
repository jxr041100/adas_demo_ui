#include <stdlib.h>
#include <android/log.h>
#include <math.h>
#include "adas.h"
#include <iostream>
#include <vector>
#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <unistd.h>
#include "kneron_adas_demo.h"

#define LOG_TAG       "kneron_adas_demo"
#define DPRINTF(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define IPRINTF(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define EPRINTF(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)


static const uint32_t MAX_FACES_TO_DETECT     = 50;
static uint32_t totalResult=0;    
CarDistance result[MAX_FACES_TO_DETECT];
CarDistance resultOld[MAX_FACES_TO_DETECT];

void copyResult(int num)
{
    for (int i=0;i<num && i<MAX_FACES_TO_DETECT;i++)
    {
        resultOld[i].x=result[i].x;
        resultOld[i].y=result[i].y;
        resultOld[i].width=result[i].width;
        resultOld[i].height=result[i].height;
        resultOld[i].distance = result[i].distance;
    }
}

int frame_number;
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
JNIEXPORT void JNICALL Java_com_asus_edrsample_kneron_adas_cleanup
(
    JNIEnv *env,
    jobject obj
)
{
    adas_free();
    return;
}

//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
JNIEXPORT void JNICALL Java_com_asus_edrsample_kneron_adas_update
(
    JNIEnv     *env,
    jobject     obj,
    jbyteArray  img,
    jint        width,
    jint        height 
)
{
    jbyte            *jimgData = NULL;
    jboolean          isCopy = 0;
    uint32_t         *curCornerPtr = 0;
    uint64_t          time;
    float             timeMs; 

    jimgData = env->GetByteArrayElements( img, &isCopy );
    uint8_t  *pJimgData = (uint8_t *)jimgData;
    if(frame_number == 0)    
        adas_init(width, height);    
    adas_car_detect((uint8_t *)pJimgData, result, &totalResult);
    copyResult(totalResult);
    frame_number++;
    env->ReleaseByteArrayElements( img, jimgData, JNI_ABORT );
}

//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
JNIEXPORT int JNICALL Java_com_asus_edrsample_kneron_adas_getNumObject
(
    JNIEnv  *env,
    jobject  obj
)
{
    return (int) totalResult;
}

JNIEXPORT void JNICALL Java_com_asus_edrsample_kneron_adas_getObjectInfo
(
    JNIEnv  *env,
    jobject  obj,
    jintArray objectRect
)
{
 
    jint pFace[MAX_FACES_TO_DETECT * 5 ];
    int outputDim = 5;
    jint size = totalResult * outputDim;   
    
    for(int i = 0; i < totalResult; i++) {
        pFace[i * outputDim + 0] = resultOld[i].x;
        pFace[i * outputDim + 1] = resultOld[i].y;
        pFace[i * outputDim + 2] = resultOld[i].width;
        pFace[i * outputDim + 3] = resultOld[i].height;
        pFace[i * outputDim + 4] = resultOld[i].distance;       
    }
    
    if (size>0)
        env->SetIntArrayRegion(objectRect, 0, size, pFace);
       
}


JNIEXPORT int JNICALL Java_com_asus_edrsample_kneron_adas_ObjectInit
(
    JNIEnv  *env,
    jobject  obj
)
{   
    frame_number = 0; 
}


JNIEXPORT int JNICALL Java_com_asus_edrsample_kneron_adas_ObjectDeinit
(
    JNIEnv  *env,
    jobject  obj
)
{
    adas_free();
    return 1;
}
#define _CLAMP(x)  (((x) & 0xFFFFFF00) ? ((~(x)>>31) & 0xFF):(x))



void yuvp2bgr565(const uint8_t  *p_y,
                  const uint8_t  *p_u,
                  const uint8_t  *p_v,
                  uint8_t  *p_bgr565,
                  uint32_t  length)
{
    int32_t i;

    int32_t C, D, E;
    int32_t sub_chroma_r, sub_chroma_g, sub_chroma_b, sub_luma;
    uint8_t R, G, B;


    for (i = length - 2; i > 0; i -= 2)
    {
        // first pixel
        C = *(p_y++);
        D = *(p_u++) - 128;
        E = *(p_v++) - 128;

        sub_chroma_r =             359 * E + 128;
        sub_chroma_g = (-88) * D - 183 * E + 128;
        sub_chroma_b =   454 * D           + 128;

        sub_luma = 256 * C;

        R = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_r) >> 8));
        G = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_g) >> 8));
        B = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_b) >> 8));

        *(p_bgr565++) = ((G << 3) & 0xE0) | (R >> 3);
        *(p_bgr565++) = ( B       & 0xF8) | (G >> 5);

        // second pixel, re-use sub_chroma
        C        = *(p_y++);
        sub_luma = 256 * C;

        R = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_r) >> 8));
        G = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_g) >> 8));
        B = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_b) >> 8));

        *(p_bgr565++) = ((G << 3) & 0xE0) | (R >> 3);
        *(p_bgr565++) = ( B       & 0xF8) | (G >> 5);
    }


    C = *(p_y++);
    D = *(p_u++) - 128;
    E = *(p_v++) - 128;

    sub_chroma_r =             359 * E + 128;
    sub_chroma_g = (-88) * D - 183 * E + 128;
    sub_chroma_b =   454 * D           + 128;

    sub_luma = 256 * C;

    R = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_r) >> 8));   
    G = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_g) >> 8));
    B = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_b) >> 8));

    *(p_bgr565++) = ((G << 3) & 0xE0) | (R >> 3);
    *(p_bgr565++) = ( B       & 0xF8) | (G >> 5);

    if (!(length & 0x01))
    {
        C        = *(p_y++);
        sub_luma = 256 * C;

        R = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_r) >> 8));       
        G = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_g) >> 8));
        B = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_b) >> 8));        

        *(p_bgr565++) = ((G << 3) & 0xE0) | (R >> 3);
        *(p_bgr565++) = ( B       & 0xF8) | (G >> 5);
    }
}



void
ColorYCbCr420PlanarToRGB565u8C( const uint8_t* __restrict srcY,
                                  const uint8_t* __restrict srcCb,
                                  const uint8_t* __restrict srcCr,
                                  uint32_t                  srcWidth,
                                  uint32_t                  srcHeight,
                                  uint32_t                  srcYStride,
                                  uint32_t                  srcCbStride,
                                  uint32_t                  srcCrStride,
                                  uint8_t* __restrict       dst,
                                  uint32_t                  dstStride )

{
    // Vertical index
    uint32_t y;

    // Line pointers
    const uint8_t* pSrcYLine;
    const uint8_t* pSrcCbLine;
    const uint8_t* pSrcCrLine;
    uint8_t* pDstLine;

    // Validate the source strides
    if (!srcYStride)
    {
        srcYStride = srcWidth * sizeof(uint8_t);
    }
    
    if (!srcCbStride)
    {

        srcCbStride = ((srcWidth + 1) / 2 ) * sizeof(uint8_t);
    }
    
    if (!srcCrStride)
    {

        srcCrStride = ((srcWidth + 1) / 2 ) * sizeof(uint8_t);
    }

    // Validate the destination stride
    if (!dstStride)
    {
        // 2 bytes per output RGB 565 pixel
        dstStride = srcWidth * sizeof(uint8_t) * 2;
    }

    // Initialize the source pointers 
    pSrcYLine  = srcY;
    pSrcCbLine = srcCb;
    pSrcCrLine = srcCr;

    // Initialize the destination pointer
    pDstLine = dst;

    for (y = 0; y < srcHeight; y += 2)
    {

        yuvp2bgr565(pSrcYLine, pSrcCbLine, pSrcCrLine, pDstLine, srcWidth);        
        pSrcYLine  += srcYStride;        
        pDstLine += dstStride;
        yuvp2bgr565(pSrcYLine, pSrcCbLine, pSrcCrLine, pDstLine, srcWidth);
        pSrcYLine  += srcYStride;
        pSrcCbLine += srcCbStride;
        pSrcCrLine += srcCrStride;
        pDstLine += dstStride;
    }
}
void yuv2bgra(const uint8_t  *p_luma,
               const uint8_t  *p_chroma,
               uint8_t  *p_bgra,
               uint32_t  length)
{
    uint32_t i;

    int32_t  C, D, E;
    int32_t  sub_chroma_r, sub_chroma_g, sub_chroma_b, sub_luma;
   

    for (i = 0; i < length - 2; i += 2)
    {
        C = *(p_luma++);
        D = *(p_chroma++) - 128;
        E = *(p_chroma++) - 128;

        sub_chroma_r =             359 * E + 128;
        sub_chroma_g = (-88) * D - 183 * E + 128;
        sub_chroma_b =   454 * D           + 128;     

        sub_luma = 256 * C;

        *(p_bgra++) = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_r) >> 8));
        *(p_bgra++) = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_g) >> 8));
        *(p_bgra++) = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_b) >> 8));
        *(p_bgra++) = 0xFF;

        C         = *(p_luma++) ;
        sub_luma  = 256 * C;

        *(p_bgra++) = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_r) >> 8));
        *(p_bgra++) = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_g) >> 8));
        *(p_bgra++) = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_b) >> 8));
        *(p_bgra++) = 0xFF;
    }

    C = *(p_luma++);
    D = *(p_chroma++) - 128;
    E = *(p_chroma++) - 128;

    sub_chroma_r =             359 * E + 128;
    sub_chroma_g = (-88) * D - 183 * E + 128;
    sub_chroma_b =   454 * D           + 128;

    sub_luma = 256 * C;

    *(p_bgra++) = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_r) >> 8));
    *(p_bgra++) = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_g) >> 8));
    *(p_bgra++) = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_b) >> 8));
    *(p_bgra++) = 0xFF;


    if (!(length & 0x01))
    {
        C         = *(p_luma++) ;
        sub_luma  = 256 * C;

        *(p_bgra++) = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_r) >> 8));
        *(p_bgra++) = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_g) >> 8));
        *(p_bgra++) = (uint8_t) (_CLAMP ((sub_luma + sub_chroma_b) >> 8));
        *(p_bgra++) = 0xFF;
    }
}



void ColorYCbCr420PseudoPlanarToRGBA8888( const uint8_t* __restrict srcY,
                                          const uint8_t* __restrict srcC,
                                          uint32_t                  srcWidth,
                                          uint32_t                  srcHeight,
                                          uint32_t                  srcYStride,
                                          uint32_t                  srcCStride,
                                          uint8_t* __restrict       dst,
                                          uint32_t                  dstStride )                                   
{
    // Vertical index
    uint32_t y;
    const uint8_t* pSrcYLine;
    const uint8_t* pSrcCLine;
    uint8_t* pDstLine;


    if (!srcYStride)
    {
        srcYStride = srcWidth * sizeof(uint8_t);
    }
    
    if (!srcCStride)    {
       
        srcCStride = srcWidth * sizeof(uint8_t);
    }

    if (!dstStride)
    {

        dstStride = srcWidth * sizeof(uint8_t) * 4;
    }
    pSrcYLine = srcY;
    pSrcCLine = srcC;
    pDstLine = dst;

    for (y = 0; y < srcHeight; y += 2)
    {       
        yuv2bgra(pSrcYLine, pSrcCLine, pDstLine, srcWidth);        
        pSrcYLine += srcYStride;      
        pDstLine += dstStride;       
        yuv2bgra(pSrcYLine, pSrcCLine, pDstLine, srcWidth);       
        pSrcYLine += srcYStride;
        pSrcCLine += srcCStride;
        pDstLine += dstStride;
    }
}


JNIEXPORT jboolean JNICALL Java_com_asus_edrsample_kneron_adas_YUV420ByteBufferToRGB8888Bitmap
        (JNIEnv *env, jobject object, jobject src, jint Coffset, jint srcWidth, jint srcHeight, jint srcYStride, jint srcCbStride, jobject outputBitmap)
{
    int *_outputBitmap;
    DPRINTF("starting to conversion 11/n");
    AndroidBitmapInfo outputBitmapInfo;

    const uint8_t *srcYPtr = (uint8_t *) env->GetDirectBufferAddress(src);
    const uint8_t *srcCbPtr = srcYPtr + Coffset;
    const uint8_t *srcCrPtr = srcCbPtr + srcCbStride * srcHeight /2;

    AndroidBitmap_getInfo(env, outputBitmap, &outputBitmapInfo);
    AndroidBitmap_lockPixels(env, outputBitmap, (void**)&_outputBitmap);
    ColorYCbCr420PlanarToRGB565u8C( 
                                        (uint8_t*)srcYPtr,
                                        (uint8_t*) srcCbPtr,
                                        (uint8_t*) srcCrPtr,
                                        srcWidth,
                                        srcHeight,
                                        srcYStride,
                                        srcCbStride,
                                        srcCbStride,
                                        (uint8_t*) _outputBitmap,
                                        outputBitmapInfo.stride ); 

    DPRINTF("end to conversion/n");
    AndroidBitmap_unlockPixels(env, outputBitmap);
    return JNI_TRUE;
}
