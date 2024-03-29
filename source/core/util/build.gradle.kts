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

    // Gson
    implementation(libs.gson)

    // libsu
    implementation(libs.libsu.core)

    // zip4j
    implementation(libs.zip4j)

    // Apache commons codec
    implementation(libs.apache.commons.codec)
}
