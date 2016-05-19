#ifndef KNERON_ADAS_DEMO_H
#define KNERON_ADAS_DEMO_H
#include <jni.h>
extern "C" {
JNIEXPORT jboolean JNICALL Java_com_asus_edrsample_kneron_adas_YUV420ByteBufferToRGB8888Bitmap
        (
        	JNIEnv *env, 
        	jobject object, 
        	jobject src, 
        	jint Coffset, 
        	jint srcWidth, 
        	jint srcHeight, 
        	jint srcYStride, 
        	jint srcCStride, 
        	jobject outputBitmap);


JNIEXPORT void JNICALL Java_com_asus_edrsample_kneron_adas_render
(
  JNIEnv * env,
  jobject obj,
  jbyteArray img,
  jint w,
  jint h
);

JNIEXPORT void JNICALL Java_com_asus_edrsample_kneron_adas_update
(
  JNIEnv*     env,
  jobject     obj,
  jbyteArray  img,
  jint        w,
  jint        h  
);


JNIEXPORT int JNICALL Java_com_asus_edrsample_kneron_adas_getNumObject
(
  JNIEnv*  env,
  jobject  obj
);

JNIEXPORT void JNICALL Java_com_asus_edrsample_kneron_adas_getObjectInfo
(
  JNIEnv*  env,
  jobject  obj,
  jintArray BoxRect
);


JNIEXPORT int JNICALL Java_com_asus_edrsample_kneron_adas_ObjectInit
(
  JNIEnv*  env,
  jobject  obj
);


JNIEXPORT int JNICALL Java_com_asus_edrsample_kneron_adas_ObjectDeinit
(
  JNIEnv*  env,
  jobject  obj
);

};

#endif 
