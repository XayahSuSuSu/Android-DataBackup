set(XDL_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/xdl/xdl/src/main/cpp")

# https://github.com/hexhacking/xDL/blob/v2.4.0/xdl/src/main/cpp/CMakeLists.txt
file(GLOB XDL_SRC "${XDL_SOURCE_DIR}/*.c")
add_library(xdl STATIC ${XDL_SRC})
target_compile_features(xdl PRIVATE c_std_17)
target_compile_options(xdl PRIVATE -std=c17 -Wall -Wextra -Werror)
target_include_directories(xdl PUBLIC "${XDL_SOURCE_DIR}/include" PRIVATE "${XDL_SOURCE_DIR}")

if (USEASAN)
    target_compile_options(xdl PRIVATE -fsanitize=address -fno-omit-frame-pointer)
    target_link_options(xdl INTERFACE -fsanitize=address)
else ()
    target_compile_options(xdl PRIVATE -Os -ffunction-sections -fdata-sections)
    # Hide symbols from libxdl.a while preserving the final shared library's JNI exports.
    target_link_options(xdl INTERFACE -Os -Wl,--exclude-libs,libxdl.a -Wl,--gc-sections)
endif ()

if ((ANDROID_ABI STREQUAL "arm64-v8a") OR (ANDROID_ABI STREQUAL "x86_64"))
    target_link_options(xdl INTERFACE "-Wl,-z,max-page-size=16384")
endif ()

# Static xDL is linked into libnativelib.so, so its objects must be position-independent.
set_target_properties(xdl PROPERTIES POSITION_INDEPENDENT_CODE ON)
# Disable NDK r25's C90 declaration-placement warning for code compiled as C17.
target_compile_options(xdl PRIVATE -Wno-declaration-after-statement)
