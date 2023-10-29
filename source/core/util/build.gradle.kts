plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
}

android {
    namespace = "com.xayah.core.util"
}

dependencies {
    // Gson
    implementation(libs.gson)

    // libsu
    implementation(libs.libsu.core)
}
