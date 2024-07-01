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

BIN_VERSION=2.0
ZLIB_VERSION=1.3.1             # https://github.com/madler/zlib/releases
XZ_VERSION=5.6.2               # https://github.com/tukaani-project/xz/releases
LZ4_VERSION=1.9.4              # https://github.com/lz4/lz4/releases
ZSTD_VERSION=1.5.6             # https://github.com/facebook/zstd/releases
TAR_VERSION=1.35               # https://ftp.gnu.org/gnu/tar/?C=M;O=D
COREUTLS_VERSION=9.4           # https://ftp.gnu.org/gnu/coreutils/?C=M;O=D
TREE_VERSION=2.1.1             # https://gitlab.com/OldManProgrammer/unix-tree
AWK_VERSION=20240422           # https://github.com/onetrueawk/awk/tags
##################################################
# Functions
set_up_utils() {
    sudo apt-get update
    sudo apt-get install wget zip unzip bzip2 -q make gcc g++ clang meson golang-go cmake bison strip-nondeterminism -y
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
    export BUILD_LDFLAGS="-s -flto -Wl,--gc-sections -Wl,--build-id=none -Wl,--hash-style=gnu"
    export BUILD_LDFLAGS_STATIC="-static $BUILD_LDFLAGS"
     
}

patch_gnu_symbols() {
    # $1: path
    sed -i "s/tzalloc/tzalloc_gnu/g" `grep tzalloc -rl $1`
    sed -i "s/tzfree/tzfree_gnu/g" `grep tzfree -rl ./`
    sed -i "s/localtime_rz/localtime_rz_gnu/g" `grep localtime_rz -rl $1`
    sed -i "s/mktime_z/mktime_z_gnu/g" `grep mktime_z -rl $1`
    sed -i "s/copy_file_range/copy_file_range_gnu/g" `grep copy_file_range -rl $1`
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

    ./configure --host=$TARGET LDFLAGS="$BUILD_LDFLAGS_STATIC" CFLAGS="$BUILD_CFLAGS -D_FORTIFY_SOURCE=0" CXXFLAGS="$BUILD_CFLAGS -D_FORTIFY_SOURCE=0" $DISABLE_YEAR2038_PARA
    make -j8
    make install prefix= DESTDIR=$LOCAL_PATH/tar
    $STRIP $LOCAL_PATH/tar/bin/tar
    cd ..
    rm -rf tar-$TAR_VERSION
}

build_coreutls() {
    # df
    if [ ! -f $LOCAL_PATH/coreutils-$COREUTLS_VERSION.tar.xz ]; then
        wget -nv https://ftp.gnu.org/gnu/coreutils/coreutils-$COREUTLS_VERSION.tar.xz
    fi
    if [ -d $LOCAL_PATH/coreutils-$COREUTLS_VERSION ]; then
        rm -rf coreutils-$COREUTLS_VERSION
    fi
    tar xf coreutils-$COREUTLS_VERSION.tar.xz
    cd coreutils-$COREUTLS_VERSION

    # Patch duplicate symbols
    patch_gnu_symbols "lib"

    ./configure --host=$TARGET LDFLAGS="$BUILD_LDFLAGS_STATIC" CFLAGS="$BUILD_CFLAGS -D_FORTIFY_SOURCE=0" CXXFLAGS="$BUILD_CFLAGS -D_FORTIFY_SOURCE=0"  $DISABLE_YEAR2038_PARA
    make -j8
    make install prefix= DESTDIR=$LOCAL_PATH/coreutls
    $STRIP $LOCAL_PATH/coreutls/bin/df
    $STRIP $LOCAL_PATH/coreutls/bin/sha1sum
    cd ..
    rm -rf coreutils-$COREUTLS_VERSION
}

build_tree() {
    # tree
    if [ ! -f $LOCAL_PATH/unix-tree-$TREE_VERSION.tar.gz ]; then
        wget -nv https://gitlab.com/OldManProgrammer/unix-tree/-/archive/$TREE_VERSION/unix-tree-$TREE_VERSION.tar.gz
    fi
    if [ -d $LOCAL_PATH/tree-$TREE_VERSION ]; then
        rm -rf $LOCAL_PATH/tree-$TREE_VERSION
    fi
    tar xf unix-tree-$TREE_VERSION.tar.gz
    cd unix-tree-$TREE_VERSION

    echo "int strverscmp (const char *s1, const char *s2);" >> tree.h
    sed -i -e "/#ifndef __linux__/d" -e "/#endif/d" strverscmp.c

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
        LDFLAGS="$BUILD_LDFLAGS_STATIC" \
        -j8
    make install prefix= DESTDIR=$LOCAL_PATH/tree MANDIR=$LOCAL_PATH/tree
    $STRIP $LOCAL_PATH/tree/tree
    cd ..
    rm -rf tree-$TREE_VERSION
}

build_awk() {
    if [ ! -f $LOCAL_PATH/$AWK_VERSION.tar.gz ]; then
        wget -nv https://github.com/onetrueawk/awk/archive/refs/tags/$AWK_VERSION.tar.gz
    fi
    if [ -d $LOCAL_PATH/awk-$AWK_VERSION ]; then
        rm -rf awk-$AWK_VERSION
    fi
    tar xf $AWK_VERSION.tar.gz
    cd awk-$AWK_VERSION

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
        LDFLAGS="$BUILD_LDFLAGS_STATIC" \
        -j8
    mkdir -p $LOCAL_PATH/awk/bin
    mv a.out $LOCAL_PATH/awk/bin/awk
    $STRIP $LOCAL_PATH/awk/bin/awk
    cd ..
    rm -rf awk-$AWK_VERSION
}

build_built_in() {
    build_zstd
    build_tar
    build_coreutls
    build_tree
    build_awk
}

package_built_in() {
    # Built-in modules
    mkdir -p built_in/$TARGET_ARCH
    echo "$BIN_VERSION" > built_in/version
    zip -pj built_in/$TARGET_ARCH/bin coreutls/bin/df coreutls/bin/sha1sum tar/bin/tar zstd/bin/zstd built_in/version tree/tree awk/bin/awk
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
    rm -rf NDK coreutls tar zstd tree awk
done
