APP_ABI := armeabi-v7a
#APP_ABI := armeabi armeabi-v7a x86
APP_STL := gnustl_static
#APP_STL := stlport_shared
#APP_STL := system
#APP_STL := libc++_static
# For APP_STL = libc++ "-std=c++11" is turned on by default.
APP_CFLAGS += -std=c++11
APP_CPPFLAGS += -fexceptions
APP_CPPFLAGS += -frtti
#APP_CPPFLAGS += -DANDROID
NDK_TOOLCHAIN_VERSION = 4.9
