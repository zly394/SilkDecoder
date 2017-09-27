# ndk 目录根据你的安装目录
ANDROID_NDK=/Users/zhuleiyue/Library/Android/sdk/ndk-bundle
# 指定 CPU 架构
CPU=arm64-v8a

# 最低支持的 Android 版本
ANDROID_API=android-18
# CPU 架构
ARCH=arch-arm
# 工具链版本
TOOLCHAIN_VERSION=4.9
# 指定工具链 CPU 架构
TOOLCHAIN_CPU=arm-linux-androideabi
# 指定编译工具 CPU 架构
CROSS_CPU=arm-linux-androideabi

ADDED_CFLAGS="-fpic -pipe "

case $CPU in
armeabi-v7a)
    ARCH=arch-arm
    TOOLCHAIN_CPU=arm-linux-androideabi
    CROSS_CPU=arm-linux-androideabi
    TARGET_ARCH=armv7-a
    ADDED_CFLAGS+="-DNO_ASM"
    ;;
arm64-v8a)
    ARCH=arch-arm64
    ANDROID_API=android-21
    TOOLCHAIN_CPU=aarch64-linux-android
    CROSS_CPU=aarch64-linux-android
    TARGET_ARCH=armv8-a
    ADDED_CFLAGS+="-D__ARMEL__"
    ;;
x86)
    ARCH=arch-x86
    TOOLCHAIN_CPU=x86
    CROSS_CPU=i686-linux-android
    TARGET_ARCH=i686
    ;;
x86_64)
    ARCH=arch-x86_64
    ANDROID_API=android-21
    TOOLCHAIN_CPU=x86_64
    CROSS_CPU=x86_64-linux-android
    TARGET_ARCH=x86-64
    ;;
*)
    echo "不支持的架构 $CPU";
    exit 1
    ;;
esac

# 设置编译针对的平台
# 最低支持的 android 版本，CPU 架构
SYSROOT=$ANDROID_NDK/platforms/$ANDROID_API/$ARCH
# 设置编译工具前缀
export TOOLCHAIN_PREFIX=$ANDROID_NDK/toolchains/$TOOLCHAIN_CPU-$TOOLCHAIN_VERSION/prebuilt/darwin-x86_64/bin/$CROSS_CPU-
# 设置编译工具后缀
export TOOLCHAIN_SUFFIX=" --sysroot=$SYSROOT"
# 设置 CPU 架构
export TARGET_ARCH
# 设置优化参数
export ADDED_CFLAGS

make clean all