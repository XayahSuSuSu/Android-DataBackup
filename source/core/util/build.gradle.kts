plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
}

android {
    namespace = "com.xayah.core.util"
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:model"))

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)

    // Gson
    implementation(libs.gson)

    // libsu
    implementation(libs.libsu.core)

    // zip4j
    implementation(libs.zip4j)

    // Apache commons codec
    implementation(libs.apache.commons.codec)

    // Work manager
    implementation(libs.androidx.work.runtime.ktx)
}
