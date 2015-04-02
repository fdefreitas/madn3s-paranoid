LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := vtk
#LOCAL_CFLAGS += -std=c++11
LOCAL_C_INCLUDES := $(ANDROID_NDK)/sources/cxx-stl/gnu-libstdc++/4.6/include
LOCAL_EXPORT_C_INCLUDES += $(LOCAL_PATH)/vtk-android/include/vtk-6.0

#Original antes de if
LOCAL_SRC_FILES := $(LOCAL_PATH)/vtk-android/lib/libvtk-android.a

#ifeq ($(TARGET_ARCH),x86)
#    LOCAL_SRC_FILES := $(LOCAL_PATH)/vtk-android/x86/lib/libvtk-android.a
#else
#    LOCAL_SRC_FILES := $(LOCAL_PATH)/vtk-android/lib/libvtk-android.a
#endif
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := json-cpp
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/json-cpp/json
LOCAL_SRC_FILES := $(LOCAL_PATH)/json-cpp/jsoncpp.cpp
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := Madn3sNative
LOCAL_STATIC_LIBRARIES := vtk
LOCAL_LDLIBS := -llog
LOCAL_EXPORT_LDLIBS := -llog
LOCAL_SRC_FILES := $(LOCAL_PATH)/madn3s/Madn3sNative.cpp
LOCAL_SHARED_LIBRARIES := json-cpp
include $(BUILD_SHARED_LIBRARY)
