#!/bin/bash

# Tested on Ubuntu22.04.1(WSL2)

# Config
# Alternative: armeabi-v7a arm64-v8a x86 x86_64

TARGET_ARCH=arm64-v8a

NDK_VERSION=r25b
XZ_VERSION=5.4.0
LZ4_VERSION=1.9.4
ZSTD_VERSION=1.5.2
TAR_VERSION=1.34
COREUTLS_VERSION=9.1
PV_VERSION=1.6.20

##################################################

# Set build target
export TARGET=aarch64-linux-android
case "$TARGET_ARCH" in
armeabi-v7a)
    export TARGET=armv7a-linux-androideabi
    ;;
arm64-v8a)
    export TARGET=aarch64-linux-android
    ;;
x86)
    export TARGET=i686-linux-android
    ;;
x86_64)
    export TARGET=x86_64-linux-android
    ;;
esac

##################################################

# Create build directory
mkdir build_bin
cd build_bin
LOCAL_PATH=$(pwd)

##################################################

# Utils
sudo apt-get update
sudo apt-get install wget zip unzip bzip2 -q make gcc g++ clang -y

##################################################

# NDK
if [ ! -f $LOCAL_PATH/android-ndk-$NDK_VERSION-linux.zip ]; then
    wget https://dl.google.com/android/repository/android-ndk-$NDK_VERSION-linux.zip
fi
if [ -d $LOCAL_PATH/NDK ]; then
    rm -rf $LOCAL_PATH/NDK
fi
unzip -q android-ndk-$NDK_VERSION-linux.zip
mv android-ndk-$NDK_VERSION NDK
export NDK=$LOCAL_PATH/NDK
export TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64
export API=28
export AR=$TOOLCHAIN/bin/llvm-ar
export CC=$TOOLCHAIN/bin/$TARGET$API-clang
export AS=$CC
export CXX=$TOOLCHAIN/bin/$TARGET$API-clang++
export LD=$TOOLCHAIN/bin/ld
export RANLIB=$TOOLCHAIN/bin/llvm-ranlib
export STRIP=$TOOLCHAIN/bin/llvm-strip

##################################################

# liblzma - for zstd
if [ ! -f $LOCAL_PATH/xz-$XZ_VERSION.tar.gz ]; then
    wget https://tukaani.org/xz/xz-$XZ_VERSION.tar.gz
fi
if [ -d $LOCAL_PATH/xz-$XZ_VERSION ]; then
    rm -rf $LOCAL_PATH/xz-$XZ_VERSION
fi
tar zxf xz-$XZ_VERSION.tar.gz
cd xz-$XZ_VERSION
./configure --host=$TARGET --prefix=$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot
make -j8 && make install -j8
cd ..
rm -rf xz-$XZ_VERSION

##################################################

# liblz4 - for zstd
if [ ! -f $LOCAL_PATH/v$LZ4_VERSION.zip ]; then
    wget https://github.com/lz4/lz4/archive/refs/tags/v$LZ4_VERSION.zip
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
    -j8
make install prefix= DESTDIR=$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot
cd ..
rm -rf lz4-$LZ4_VERSION

##################################################

# zstd
if [ ! -f $LOCAL_PATH/zstd-$ZSTD_VERSION.tar.gz ]; then
    wget https://github.com/facebook/zstd/releases/download/v$ZSTD_VERSION/zstd-$ZSTD_VERSION.tar.gz
fi
if [ -d $LOCAL_PATH/zstd-$ZSTD_VERSION ]; then
    rm -rf $LOCAL_PATH/zstd-$ZSTD_VERSION
fi
tar zxf zstd-$ZSTD_VERSION.tar.gz
cd zstd-$ZSTD_VERSION
LDFLAGS="--static -O3 -flto -DZSTD_MULTITHREAD=1 -pthread -ffunction-sections -fdata-sections -Wl,--gc-sections" make \
    AR=$AR \
    CC=$CC \
    AS=$AS \
    CXX=$CXX \
    LD=$LD \
    RANLIB=$RANLIB \
    STRIP=$STRIP \
    -j8

make install prefix= DESTDIR=$LOCAL_PATH/zstd
$STRIP $LOCAL_PATH/zstd/bin/zstd
cd ..
rm -rf zstd-$ZSTD_VERSION

##################################################

# tar
if [ ! -f $LOCAL_PATH/tar-$TAR_VERSION.tar.xz ]; then
    wget https://ftp.gnu.org/gnu/tar/tar-$TAR_VERSION.tar.xz
fi
if [ -d $LOCAL_PATH/tar-$TAR_VERSION ]; then
    rm -rf tar-$TAR_VERSION
fi
tar xf tar-$TAR_VERSION.tar.xz
cd tar-1.34
./configure --host=$TARGET CFLAGS="-O3 -flto -D_FORTIFY_SOURCE=0"
make -j8
make install prefix= DESTDIR=$LOCAL_PATH/tar
$STRIP $LOCAL_PATH/tar/bin/tar
cd ..
rm -rf tar-$TAR_VERSION

##################################################

# coreutls - df
if [ ! -f $LOCAL_PATH/coreutils-$COREUTLS_VERSION.tar.xz ]; then
    wget https://ftp.gnu.org/gnu/coreutils/coreutils-$COREUTLS_VERSION.tar.xz
fi
if [ -d $LOCAL_PATH/coreutils-$COREUTLS_VERSION ]; then
    rm -rf coreutils-$COREUTLS_VERSION
fi
tar xf coreutils-$COREUTLS_VERSION.tar.xz
cd coreutils-$COREUTLS_VERSION
./configure --host=$TARGET --prefix= CFLAGS="-O3 -flto -D_FORTIFY_SOURCE=0"
make -j8
make install prefix= DESTDIR=$LOCAL_PATH/coreutls
$STRIP $LOCAL_PATH/coreutls/bin/df
cd ..
rm -rf coreutils-$COREUTLS_VERSION

##################################################

# pv
if [ ! -f $LOCAL_PATH/pv-$PV_VERSION.tar.gz ]; then
    wget https://github.com/a-j-wood/pv/releases/download/v$PV_VERSION/pv-$PV_VERSION.tar.gz
fi
if [ -d $LOCAL_PATH/pv-$PV_VERSION ]; then
    rm -rf pv-$PV_VERSION
fi
tar zxvf pv-$PV_VERSION.tar.gz
cd pv-$PV_VERSION
./configure --host=$TARGET --prefix= CFLAGS="-O3 -flto"
make -j8
make install DESTDIR=$LOCAL_PATH/pv
$STRIP $LOCAL_PATH/pv/bin/pv
cd ..
rm -rf pv-$PV_VERSION

##################################################

# Package needed files

mkdir $TARGET_ARCH
zip -pj $TARGET_ARCH/bin coreutls/bin/df pv/bin/pv tar/bin/tar zstd/bin/zstd

##################################################

# Clean build files
rm -rf NDK coreutls pv tar zstd
