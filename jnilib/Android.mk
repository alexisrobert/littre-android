LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := littre
LOCAL_SRC_FILES := littre.c
LOCAL_LDLIBS    := -L$(SYSROOT)/usr/lib -llog 

include $(BUILD_SHARED_LIBRARY)
