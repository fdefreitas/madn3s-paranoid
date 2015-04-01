APP_ABI := armeabi-v7a armeabi
#APP_ABI := armeabi-v7a x86
APP_STL := gnustl_static
# For APP_STL = libc++ "-std=c++11" is turned on by default.
APP_CFLAGS += -std=c++11
APP_CPPFLAGS += -fexceptions
APP_CPPFLAGS += -frtti
#APP_CPPFLAGS += -DANDROID
NDK_TOOLCHAIN_VERSION = 4.9
