# The setting can also ne defined in build.gradle ndk 'APP_PLATFORM=android-15' which takes priority over this value
# APP_PLATFORM=android-15 for aTalk minimum support SDK platform i.e. api-15
# see https://github.com/android-ndk/ndk/issues/543
# https://android.googlesource.com/platform/ndk/+/master/docs/user/common_problems.md#using-mismatched-prebuilt-libraries
APP_PLATFORM := android-21

# https://developer.android.com/ndk/guides/abis.html
APP_ABI := all

# APP_STL := gnustl_static |  c++_static | c++_shared
APP_STL := c++_static

# Enforced the support for Exceptions and RTTI in all generated machine code.
APP_CPPFLAGS := -fexceptions

# Compile app using 16 KB ELF alignment
APP_SUPPORT_FLEXIBLE_PAGE_SIZES := true

# https://developer.android.com/ndk/guides/ndk-build.html #Debuggable versus Release builds
# Automatically set by sdk build - SDK r8 (or higher)
# Table 1. Results of NDK_DEBUG (command line) and android:debuggable (manifest) combinations.
# Manifest Setting        	NDK_DEBUG=0 	NDK_DEBUG=1	    NDK_DEBUG not specified
#android:debuggable="true" 	Debug; Symbols; Optimized*1 	Debug; Symbols; Not optimized*2	(same as NDK_DEBUG=1)
#android:debuggable="false"	Release; Symbols; Optimized 	Release; Symbols; Not optimized	Release; No symbols; Optimized*3
NDK_DEBUG := false
NDK_TOOLCHAIN_VERSION := clang