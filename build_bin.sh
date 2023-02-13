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
LIBFUSE_VERSION=3.12.0
RCLONE_VERSION=1.61.1
EXTEND_VERSION=1.1.1

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
sudo apt-get install wget zip unzip bzip2 -q make gcc g++ clang meson golang-go -y

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

# fusermount - extend module
if [ ! -f $LOCAL_PATH/fuse-$LIBFUSE_VERSION.tar.xz ]; then
    wget https://github.com/libfuse/libfuse/releases/download/fuse-$LIBFUSE_VERSION/fuse-$LIBFUSE_VERSION.tar.xz
fi
if [ -d $LOCAL_PATH/fuse-$LIBFUSE_VERSION ]; then
    rm -rf fuse-$LIBFUSE_VERSION
fi
tar xf fuse-$LIBFUSE_VERSION.tar.xz
cd fuse-$LIBFUSE_VERSION
sed -i '/# Read build files from sub-directories/, $d' meson.build
echo "subdir('util')" >> meson.build
sed -i '/mount.fuse3/, $d' util/meson.build
mkdir build && cd build

export FUSE_HOST=aarch64
case "$TARGET_ARCH" in
armeabi-v7a)
    export FUSE_HOST=armv7a
    ;;
arm64-v8a)
    export FUSE_HOST=aarch64
    ;;
x86)
    export FUSE_HOST=i686
    ;;
x86_64)
    export FUSE_HOST=x86_64
    ;;
esac

echo -e "[binaries]\n\
c = '$CC'\n\
cpp = '$CXX'\n\
ar = '$AR'\n\
ld = '$LD'\n\
strip = '$STRIP'\n\n\
[host_machine]\n\
system = 'android'\n\
cpu_family = '$FUSE_HOST'\n\
cpu = '$FUSE_HOST'\n\
endian = 'little'" > cross_config

meson .. $FUSE_HOST --cross-file cross_config --prefix=/
ninja -C $FUSE_HOST
DESTDIR=$LOCAL_PATH/fuse ninja -C $FUSE_HOST install
$STRIP $LOCAL_PATH/fuse/bin/fusermount3
mv $LOCAL_PATH/fuse/bin/fusermount3 $LOCAL_PATH/fuse/bin/fusermount
cd ../../
rm -rf fuse-$LIBFUSE_VERSION

##################################################

# rclone - extend module
if [ ! -f $LOCAL_PATH/rclone-v$RCLONE_VERSION.tar.gz ]; then
    wget https://github.com/rclone/rclone/releases/download/v$RCLONE_VERSION/rclone-v$RCLONE_VERSION.tar.gz
fi
if [ -d $LOCAL_PATH/rclone-v$RCLONE_VERSION ]; then
    rm -rf rclone-v$RCLONE_VERSION
fi
tar zxf rclone-v$RCLONE_VERSION.tar.gz
cd rclone-v$RCLONE_VERSION

export VAR_GOARCH=arm64
case "$TARGET_ARCH" in
armeabi-v7a)
    export VAR_GOARCH=arm
    ;;
arm64-v8a)
    export VAR_GOARCH=arm64
    ;;
x86)
    export VAR_GOARCH=386
    ;;
x86_64)
    export VAR_GOARCH=amd64
    ;;
esac

mkdir $LOCAL_PATH/rclone
CGO_ENABLED=1 CC=$TOOLCHAIN/bin/$TARGET$API-clang GOOS=android GOARCH=$VAR_GOARCH go build -o $LOCAL_PATH/rclone
$STRIP $LOCAL_PATH/rclone/rclone
cd ..
rm -rf rclone-v$RCLONE_VERSION

##################################################

# Package needed files

# Built-in modules
mkdir -p built_in/$TARGET_ARCH
zip -pj built_in/$TARGET_ARCH/bin coreutls/bin/df tar/bin/tar zstd/bin/zstd

# Extend modules
mkdir -p extend
echo "$EXTEND_VERSION" > extend/version
zip -pj extend/$TARGET_ARCH fuse/bin/fusermount rclone/rclone extend/version

##################################################

# Clean build files
rm -rf NDK coreutls tar zstd fuse rclone
