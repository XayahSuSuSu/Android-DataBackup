plugins {
    alias(libs.plugins.library.common)
}

android {
    namespace = "com.xayah.feature.flavor.foss"
}

dependencies {
    // Core
    implementation(project(":core:provider"))
}
