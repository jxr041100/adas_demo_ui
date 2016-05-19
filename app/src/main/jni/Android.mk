LOCAL_PATH:= $(call my-dir)


include $(CLEAR_VARS)
LOCAL_MODULE := libkneron_adas
LOCAL_SRC_FILES := kneron/lib/libkneron_adas.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_PRELINK_MODULE:= false

LOCAL_MODULE    := libkneron_demo
LOCAL_SRC_FILES := kneron_adas_demo.cpp 
LOCAL_C_INCLUDES +=  $(JNI_H_INCLUDE) \
					  kneron/include \
					  fastcv/inc

LOCAL_LDLIBS +=  -llog -ldl -ljnigraphics

LOCAL_STATIC_LIBRARIES += libkneron_adas

include $(BUILD_SHARED_LIBRARY)


