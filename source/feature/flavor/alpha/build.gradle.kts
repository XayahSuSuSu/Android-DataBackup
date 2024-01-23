plugins {
    alias(libs.plugins.library.common)
}

android {
    namespace = "com.xayah.feature.flavor.alpha"
}

dependencies {
    // Core
    implementation(project(":core:provider"))
}
