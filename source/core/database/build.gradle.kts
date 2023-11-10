plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
    alias(libs.plugins.library.room)
    alias(libs.plugins.library.protobuf)
}

android {
    namespace = "com.xayah.core.database"
}

dependencies {
    implementation(libs.androidx.activity.compose)

    // Core
    implementation(project(":core:model"))
    implementation(project(":core:util"))

    // Gson
    implementation(libs.gson)
}
