plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.compose)
}

android {
    namespace = "com.xayah.feature.home.foss"
}

dependencies {
    // Core
    implementation(project(":core:ui"))
}