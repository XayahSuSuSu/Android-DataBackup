#!/bin/bash

set -eo pipefail

# Tested on Ubuntu26.04 (WSL2)

# Config
# $abis: all or abis(armeabi-v7a, arm64-v8a, x86, x86_64) split by `,`
# `bash build_bin.sh $abis`
# e.g. `bash build_bin.sh x86,x86_64`

API=24
NDK_VERSION=r30
TERMUX_PACKAGES_VERSION=bootstrap-2026.09.13-r1+apt.android-7       # https://github.com/termux/termux-packages/releases
TAR_VERSION=1.35                                                    # https://ftp.gnu.org/gnu/tar/?C=M;O=D
##################################################
# Functions
set_up_utils() {
    sudo apt-get update
    sudo apt-get install wget zip unzip bzip2 -q make gcc g++ clang meson golang-go cmake bison strip-nondeterminism -y
    # Create build directory
    mkdir -p build_bin
    cd build_bin
    export LOCAL_PATH=$(pwd)
}

set_up_environment() {
    export DISABLE_YEAR2038_PARA=""
    # Set build target
    export TARGET=aarch64-linux-android
    case "$TARGET_ARCH" in

    # DISABLE_YEAR2038_PARA: Workaround for https://github.com/msys2/MSYS2-packages/pull/4080
    armeabi-v7a)
        export TARGET=armv7a-linux-androideabi
        export DISABLE_YEAR2038_PARA=--disable-year2038
        ;;
    arm64-v8a)
        export TARGET=aarch64-linux-android
        ;;
    x86)
        export TARGET=i686-linux-android
        export DISABLE_YEAR2038_PARA=--disable-year2038
        ;;
    x86_64)
        export TARGET=x86_64-linux-android
        ;;
    esac

    # NDK
    if [ ! -f $LOCAL_PATH/android-ndk-$NDK_VERSION-linux.zip ]; then
        wget -nv https://dl.google.com/android/repository/android-ndk-$NDK_VERSION-linux.zip
    fi
    if [ -d $LOCAL_PATH/NDK ]; then
        rm -rf $LOCAL_PATH/NDK
    fi
    unzip -q android-ndk-$NDK_VERSION-linux.zip
    mv android-ndk-$NDK_VERSION NDK
    export NDK=$LOCAL_PATH/NDK
    export TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64
    export SYSROOT=$TOOLCHAIN/sysroot
    export API=$API
    export AR=$TOOLCHAIN/bin/llvm-ar
    export CC=$TOOLCHAIN/bin/$TARGET$API-clang
    export AS=$CC
    export CXX=$TOOLCHAIN/bin/$TARGET$API-clang++
    export LD=$TOOLCHAIN/bin/ld
    export RANLIB=$TOOLCHAIN/bin/llvm-ranlib
    export STRIP=$TOOLCHAIN/bin/llvm-strip
    export FILE_PREFIX_MAP=/src
    export BUILD_CFLAGS="-O3 -ffunction-sections -fdata-sections -ffile-prefix-map=$LOCAL_PATH=$FILE_PREFIX_MAP"
    export BUILD_LDFLAGS="-s -flto -Wl,--gc-sections -Wl,--build-id=none -Wl,--hash-style=both"
    export BUILD_LDFLAGS_STATIC="-static $BUILD_LDFLAGS"
     
}

patch_gnu_symbols() {
    # $1: path
    sed -i "s/tzalloc/tzalloc_gnu/g" `grep tzalloc -rl $1`
    sed -i "s/tzfree/tzfree_gnu/g" `grep tzfree -rl ./`
    sed -i "s/localtime_rz/localtime_rz_gnu/g" `grep localtime_rz -rl $1`
    sed -i "s/mktime_z/mktime_z_gnu/g" `grep mktime_z -rl $1`
    sed -i "s/copy_file_range/copy_file_range_gnu/g" `grep copy_file_range -rl $1`
    sed -i '/^typedef struct tm_zone \*timezone_t;/i #define timezone_t rpl_timezone_t' "$1/time.in.h"
}

build_libandroid_glob() {
    # For tar (Android API 24)
    local archive="termux-packages-$TERMUX_PACKAGES_VERSION.zip"
    local lib_target="$TARGET"

    cd "$LOCAL_PATH"
    if [ ! -s "$archive" ]; then
        wget -nv -O "$archive.tmp" "https://github.com/termux/termux-packages/archive/refs/tags/$TERMUX_PACKAGES_VERSION.zip"
        mv "$archive.tmp" "$archive"
    fi

    mkdir -p "$LOCAL_PATH/android-glob/$TARGET_ARCH"
    unzip -q -j -o "$archive" \
        '*/packages/libandroid-glob/glob.c' \
        '*/packages/libandroid-glob/glob.h' \
        '*/packages/libandroid-glob/LICENSE' \
        -d "$LOCAL_PATH/android-glob/$TARGET_ARCH"
    cd "$LOCAL_PATH/android-glob/$TARGET_ARCH"

    "$CC" \
        $BUILD_CFLAGS \
        -fPIC \
        -I. \
        -c glob.c \
        -o glob.o
    "$AR" rcs libandroid-glob.a glob.o
    "$RANLIB" libandroid-glob.a

    # NDK library directory for armeabi-v7a
    if [ "$TARGET_ARCH" = armeabi-v7a ]; then
        lib_target=arm-linux-androideabi
    fi

    install -Dm644 glob.h "$SYSROOT/usr/include/glob.h"
    install -Dm644 libandroid-glob.a "$SYSROOT/usr/lib/$lib_target/libandroid-glob.a"

    cd "$LOCAL_PATH"
}

build_tar() {
    if [ ! -f $LOCAL_PATH/tar-$TAR_VERSION.tar.xz ]; then
        wget -nv https://ftp.gnu.org/gnu/tar/tar-$TAR_VERSION.tar.xz
    fi
    if [ -d $LOCAL_PATH/tar-$TAR_VERSION ]; then
        rm -rf tar-$TAR_VERSION
    fi
    tar xf tar-$TAR_VERSION.tar.xz
    cd tar-$TAR_VERSION

    # Patch duplicate symbols
    patch_gnu_symbols "gnu"

    # Force use of the time zone functions provided by gnulib.
    ac_cv_type_timezone_t=no \
    ac_cv_func_lchmod=no \
    ./configure \
        --host="$TARGET" \
        LIBS="-landroid-glob" \
        LDFLAGS="$BUILD_LDFLAGS_STATIC" \
        CFLAGS="$BUILD_CFLAGS -D_FORTIFY_SOURCE=0" \
        CXXFLAGS="$BUILD_CFLAGS -D_FORTIFY_SOURCE=0" \
        $DISABLE_YEAR2038_PARA
    make -j8
    make install prefix= DESTDIR=$LOCAL_PATH/tar
    cd "$LOCAL_PATH"
}

##################################################

# Start to build
set_up_utils

if [[ $1 == all ]]; then
    abis=("armeabi-v7a" "arm64-v8a" "x86" "x86_64")
else
    PRESERVED_IFS="$IFS"
    IFS=","
    abis=($1)
    IFS="$PRESERVED_IFS"
fi

for abi in ${abis[@]}; do
    TARGET_ARCH=$abi
    set_up_environment
    build_libandroid_glob
    build_tar
done
