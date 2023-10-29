plugins {
    alias(libs.plugins.library.common)
}

android {
    namespace = "com.xayah.core.model"
}

dependencies {
    // Core
    implementation(project(":core:util"))
}
