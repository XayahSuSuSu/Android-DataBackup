plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.compose)
    alias(libs.plugins.library.hilt)
}

android {
    namespace = "com.xayah.core.ui"
}

dependencies {
    // Core
    implementation(project(":core:model"))

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Accompanist
    implementation(libs.accompanist.placeholder)
}
