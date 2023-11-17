plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
    alias(libs.plugins.library.compose)
}

android {
    namespace = "com.xayah.feature.guide.common"
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
}