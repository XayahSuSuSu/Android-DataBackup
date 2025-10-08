set(CMAKE_INSTALL_PREFIX ${CMAKE_CURRENT_SOURCE_DIR})

add_compile_options(
        -O3
        -fPIC
        -ffunction-sections
        -fdata-sections
        -D_FORTIFY_SOURCE=0
        -ffile-prefix-map=${CMAKE_CURRENT_SOURCE_DIR}=/src
        -DZSTD_LEGACY_SUPPORT=4
        -DZSTD_MULTITHREAD=1
)

add_link_options(
        -s
        -flto
        -Wl,--gc-sections
        -Wl,--build-id=none
        -Wl,--hash-style=both
)

file(READ zstd-jni/version ZSTD_VERSION)
string(STRIP "${ZSTD_VERSION}" ZSTD_VERSION)
set(ZSTD_NAME zstd-jni-${ZSTD_VERSION})

# https://github.com/luben/zstd-jni/blob/97b5262284ad6b850c851c60ace2a4a93d9720e4/make_so_cross.sh
file(GLOB_RECURSE ZSTD_JNI_SOURCES
        zstd-jni/src/main/native/*.c
        zstd-jni/src/main/native/common/*.c
        zstd-jni/src/main/native/compress/*.c
        zstd-jni/src/main/native/decompress/*.c
)
add_library(${ZSTD_NAME} SHARED
        ${ZSTD_JNI_SOURCES}
)
if (CMAKE_ANDROID_ARCH_ABI STREQUAL "x86_64")
    enable_language(ASM)
    target_sources(${ZSTD_NAME}
            PRIVATE
            zstd-jni/src/main/native/decompress/huf_decompress_amd64.S
    )
endif ()
target_include_directories(${ZSTD_NAME}
        PRIVATE
        zstd-jni/src/main/native
        zstd-jni/src/main/native/common
)
