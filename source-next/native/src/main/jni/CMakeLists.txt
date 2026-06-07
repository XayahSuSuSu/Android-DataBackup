cmake_minimum_required(VERSION 3.22.1)

project("native")

add_link_options(
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384"
)

add_subdirectory(nativelib)
add_subdirectory(rustic)
add_subdirectory(external)
