plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.room)
    alias(libs.plugins.library.test)
    alias(libs.plugins.library.protobuf)
}

android {
    namespace = "com.xayah.core.model"
}

dependencies {
    // Core
    implementation(project(":core:common"))

    // Gson
    implementation(libs.gson)
}
