#!/bin/bash

# Tested on Ubuntu22.04.1(WSL2)

# Config
# $type: built-in, all
# $abis: all or abis(armeabi-v7a, arm64-v8a, x86, x86_64) split by `,`
# `bash build_bin.sh $type $abis`
# e.g. `bash build_bin.sh built-in x86,x86_64`

# Whether use dev branch instead of release
ZSTD_DEV=false

NDK_VERSION=r25c

BIN_VERSION=2.1
ZLIB_VERSION=1.3.1                                               # https://github.com/madler/zlib/releases
XZ_VERSION=5.6.2                                                 # https://github.com/tukaani-project/xz/releases
LZ4_VERSION=1.9.4                                                # https://github.com/lz4/lz4/releases
ZSTD_VERSION=1.5.6                                               # https://github.com/facebook/zstd/releases
BUSYBOX_VERSION=1_36_1                                           # https://www.busybox.net/downloads/?C=M;O=D
SELINUX_COMMIT=81d604a9d5f34306d22121391d350e0027191cb5          # https://github.com/XayahSuSuSu/selinux/tree/81d604a9d5f34306d22121391d350e0027191cb5
PCRE_BRANCH=android14-mainline-adbd-release                      # https://android.googlesource.com/platform/external/pcre
##################################################
# Functions
set_up_utils() {
    sudo apt-get update
    sudo apt-get install wget zip unzip bzip2 make meson cmake bison strip-nondeterminism xz-utils -y
    # Create build directory
    mkdir build_bin
    cd build_bin
    export LOCAL_PATH=$(pwd)
}

set_up_environment() {
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
    if [ -z $NDK ];then
        if [ ! -f $LOCAL_PATH/android-ndk-$NDK_VERSION-linux.zip ]; then
            wget -nv https://dl.google.com/android/repository/android-ndk-$NDK_VERSION-linux.zip
        fi
        if [ -d $LOCAL_PATH/NDK ]; then
            rm -rf $LOCAL_PATH/NDK
        fi
        unzip -q android-ndk-$NDK_VERSION-linux.zip
        mv android-ndk-$NDK_VERSION NDK
        export NDK=$LOCAL_PATH/NDK
        export USE_NEW_NDK=true
    fi
    export PATH=$NDK:$PATH
    export TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64
    export SYSROOT=$TOOLCHAIN/sysroot
    export API=28
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
    export FORCE_UNSAFE_CONFIGURE=1
    export KCONFIG_NOTIMESTAMP=1 # Busybox
}

build_zlib() {
    # For zstd
    if [ ! -f $LOCAL_PATH/zlib-$ZLIB_VERSION.tar.gz ]; then
        wget -nv https://github.com/madler/zlib/releases/download/v$ZLIB_VERSION/zlib-$ZLIB_VERSION.tar.gz
    fi
    if [ -d $LOCAL_PATH/zlib-$ZLIB_VERSION ]; then
        rm -rf $LOCAL_PATH/zlib-$ZLIB_VERSION
    fi
    tar zxf zlib-$ZLIB_VERSION.tar.gz
    cd zlib-$ZLIB_VERSION
    ./configure --prefix=$SYSROOT
    make \
        AR=$AR \
        CC=$CC \
        AS=$AS \
        CXX=$CXX \
        LD=$LD \
        RANLIB=$RANLIB \
        STRIP=$STRIP \
        CFLAGS="$BUILD_CFLAGS" \
        CXXFLAGS="$BUILD_CFLAGS" \
        -j8
    make install -j8
    cd ..
    rm -rf zlib-$ZLIB_VERSION
}

build_liblzma() {
    # For zstd
    if [ ! -f $LOCAL_PATH/xz-$XZ_VERSION.tar.gz ]; then
        wget -nv https://github.com/tukaani-project/xz/releases/download/v$XZ_VERSION/xz-$XZ_VERSION.tar.gz
    fi
    if [ -d $LOCAL_PATH/xz-$XZ_VERSION ]; then
        rm -rf $LOCAL_PATH/xz-$XZ_VERSION
    fi
    tar zxf xz-$XZ_VERSION.tar.gz
    cd xz-$XZ_VERSION
    ./configure --host=$TARGET --prefix=$SYSROOT CFLAGS="$BUILD_CFLAGS" CXXFLAGS="$BUILD_CFLAGS"
    make -j8 && make install -j8
    cd ..
    rm -rf xz-$XZ_VERSION
}

build_liblz4() {
    # For zstd
    if [ ! -f $LOCAL_PATH/v$LZ4_VERSION.zip ]; then
        wget -nv https://github.com/lz4/lz4/archive/refs/tags/v$LZ4_VERSION.zip
    fi
    if [ -d $LOCAL_PATH/lz4-$LZ4_VERSION ]; then
        rm -rf $LOCAL_PATH/lz4-$LZ4_VERSION
    fi
    unzip -q v$LZ4_VERSION.zip
    cd lz4-$LZ4_VERSION
    make \
        AR=$AR \
        CC=$CC \
        AS=$AS \
        CXX=$CXX \
        LD=$LD \
        RANLIB=$RANLIB \
        STRIP=$STRIP \
        CFLAGS="$BUILD_CFLAGS" \
        CXXFLAGS="$BUILD_CFLAGS" \
        -j8
    make install prefix= DESTDIR=$SYSROOT
    cd ..
    rm -rf lz4-$LZ4_VERSION
}

build_zstd() {
    # Build needed libs
    build_zlib
    build_liblzma
    build_liblz4
    # Remove all shared libs
    rm -rf $SYSROOT/lib/*.so*
    rm -rf $SYSROOT/usr/lib/*/libz*
    rm -rf $SYSROOT/usr/lib/*/*/libz*

    if [ $ZSTD_DEV == true ]; then
        ZSTD_VERSION=dev
        if [ -d $LOCAL_PATH/zstd-$ZSTD_VERSION ]; then
            rm -rf $LOCAL_PATH/zstd-$ZSTD_VERSION
        fi
        git clone https://jihulab.com/XayahSuSuSu/zstd -b dev zstd-$ZSTD_VERSION
    else
        if [ ! -f $LOCAL_PATH/zstd-$ZSTD_VERSION.tar.gz ]; then
            wget -nv https://github.com/facebook/zstd/releases/download/v$ZSTD_VERSION/zstd-$ZSTD_VERSION.tar.gz
        fi
        if [ -d $LOCAL_PATH/zstd-$ZSTD_VERSION ]; then
            rm -rf $LOCAL_PATH/zstd-$ZSTD_VERSION
        fi
        tar zxf zstd-$ZSTD_VERSION.tar.gz
    fi

    cd zstd-$ZSTD_VERSION/build/cmake
    mkdir builddir && cd builddir
    cmake \
    -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI=$TARGET_ARCH \
    -DANDROID_NATIVE_API_LEVEL=$API \
    -DZSTD_BUILD_STATIC=ON \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX= \
    -DZSTD_MULTITHREAD_SUPPORT=ON \
    -DZSTD_ZLIB_SUPPORT=ON \
    -DZSTD_LZMA_SUPPORT=ON \
    -DZSTD_LZ4_SUPPORT=ON \
    -DZSTD_LEGACY_SUPPORT=OFF \
    -DCMAKE_EXE_LINKER_FLAGS="$BUILD_LDFLAGS_STATIC" \
    -DCMAKE_C_FLAGS="${CMAKE_C_FLAGS} $BUILD_CFLAGS" \
    -DCMAKE_CXX_FLAGS="${CMAKE_C_FLAGS} $BUILD_CFLAGS" \
    ..
    make -j8
    make install prefix= DESTDIR=$LOCAL_PATH/zstd

    $STRIP $LOCAL_PATH/zstd/bin/zstd
    cd ../../../..
    rm -rf zstd-$ZSTD_VERSION
}

build_external() {
    cp -r $ROOT_PATH/../source/native/src/main/jni jni
    cd jni
    sed -i '/^# add_subdirectory(external)/s/^# //' CMakeLists.txt
    sed -i '/^add_subdirectory(nativelib)/s/^/# /' CMakeLists.txt
    mkdir builddir && cd builddir
    cmake \
    -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI=$TARGET_ARCH \
    -DANDROID_NATIVE_API_LEVEL=$API \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX= \
    -DCMAKE_EXE_LINKER_FLAGS="$BUILD_LDFLAGS_STATIC" \
    -DCMAKE_C_FLAGS="${CMAKE_C_FLAGS} $BUILD_CFLAGS" \
    -DCMAKE_CXX_FLAGS="${CMAKE_C_FLAGS} $BUILD_CFLAGS" \
    ..
    make -j8
    make install prefix= DESTDIR=$LOCAL_PATH/external

    $STRIP $LOCAL_PATH/external/bin/tar
    cd ../../
    rm -rf jni
}

build_busybox() {
    git clone https://github.com/XayahSuSuSu/ndk-box-kitchen -b $NDK_VERSION && cd ndk-box-kitchen
    wget https://git.busybox.net/busybox/snapshot/busybox-$BUSYBOX_VERSION.tar.bz2
    tar xf busybox-$BUSYBOX_VERSION.tar.bz2
    mv busybox-$BUSYBOX_VERSION busybox
    git clone https://github.com/XayahSuSuSu/selinux jni/selinux
    cd jni/selinux && git checkout $SELINUX_COMMIT && cd ../..
    git clone https://android.googlesource.com/platform/external/pcre -b $PCRE_BRANCH jni/pcre
    ./run.sh patch
    ./run.sh generate
    ndk-build APP_ABI=$TARGET_ARCH
    mkdir -p $LOCAL_PATH/busybox/bin
    mv libs/$TARGET_ARCH/busybox $LOCAL_PATH/busybox/bin/busybox
    $STRIP $LOCAL_PATH/busybox/bin/busybox
    cd ..
    rm -rf ndk-box-kitchen
}

build_built_in() {
    build_zstd
    build_busybox
    build_external
}

package_built_in() {
    # Built-in modules
    mkdir -p built_in/$TARGET_ARCH
    echo "$BIN_VERSION" > built_in/version
    zip -pj built_in/$TARGET_ARCH/bin built_in/version zstd/bin/zstd busybox/bin/busybox external/bin/tar
    strip-nondeterminism built_in/$TARGET_ARCH/bin.zip
}

build() {
    # $1: type
    case "$1" in
    built-in)
        build_built_in
        ;;
    *)
        build_built_in
        ;;
    esac
}
 
package() {
    # $1: type
    case "$1" in
    built-in)
        package_built_in
        ;;
    *)
        package_built_in
        ;;
    esac
}
##################################################
ROOT_PATH=$(dirname $(readlink -f "$0"))
# Start to build
set_up_utils

if [[ $2 == all ]]; then
    abis=("armeabi-v7a" "arm64-v8a" "x86" "x86_64")
else
    PRESERVED_IFS="$IFS"
    IFS=","
    abis=($2)
    IFS="$PRESERVED_IFS"
fi

for abi in ${abis[@]}; do
    TARGET_ARCH=$abi
    set_up_environment
    build $1
    package $1
    # Clean build files
    rm -rf NDK zstd busybox
    if [ ! -z $USE_NEW_NDK ];then
        unset NDK
    fi
done

