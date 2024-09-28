cmake_minimum_required(VERSION 3.22.1)

project("nativelib")

add_library(${CMAKE_PROJECT_NAME} SHARED
        nativelib.cpp
)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
)

target_compile_options(${CMAKE_PROJECT_NAME} PRIVATE -O3 -ffunction-sections -fdata-sections -ffile-prefix-map=${CMAKE_CURRENT_SOURCE_DIR}=/src)
target_link_libraries(${CMAKE_PROJECT_NAME} -s -flto -Wl,--gc-sections -Wl,--build-id=none -Wl,--hash-style=both)
