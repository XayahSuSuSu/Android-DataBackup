add_compile_options(
        -O3
        -fPIC
        -ffunction-sections
        -fdata-sections
        -D_FORTIFY_SOURCE=0
        -ffile-prefix-map=${CMAKE_CURRENT_SOURCE_DIR}=/src
)

add_link_options(
        -s
        -flto
        -Wl,--gc-sections
        -Wl,--build-id=none
        -Wl,--hash-style=both
)

add_library(nativelib SHARED
        nativelib.cpp
)

target_link_libraries(nativelib
        android
        log
)
