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

include(FetchContent)
set(Rust_RUSTUP_INSTALL_MISSING_TARGET ON CACHE BOOL "Install missing Rust targets through rustup" FORCE)
FetchContent_Declare(
        Corrosion
        GIT_REPOSITORY https://github.com/corrosion-rs/corrosion.git
        GIT_TAG v0.6.1
        GIT_SHALLOW 1
)
FetchContent_MakeAvailable(Corrosion)

corrosion_import_crate(
        MANIFEST_PATH Cargo.toml
        LOCKED
)

# librustic.so is produced by linking the Rust staticlib into this JNI anchor.
add_library(rustic-jni SHARED
        rustic.cpp
)

set_target_properties(rustic-jni PROPERTIES
        OUTPUT_NAME rustic
)

target_link_options(rustic-jni PRIVATE
        "-Wl,--version-script=${CMAKE_CURRENT_SOURCE_DIR}/rustic.map"
)

target_link_libraries(rustic-jni
        android
        log
        -Wl,--whole-archive
        rustic-static
        -Wl,--no-whole-archive
)
