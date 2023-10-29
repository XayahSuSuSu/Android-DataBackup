plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.compose)
}

android {
    namespace = "com.xayah.feature.home.premium"
}

dependencies {
    // Core
    implementation(project(":core:ui"))
}