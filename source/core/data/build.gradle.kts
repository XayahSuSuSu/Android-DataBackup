plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
    alias(libs.plugins.library.protobuf)
    alias(libs.plugins.library.androidTest)
    alias(libs.plugins.library.compose)
}

android {
    namespace = "com.xayah.core.data"
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:util"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(project(":core:ui"))
    implementation(project(":core:rootservice"))
    implementation(project(":core:network"))
    implementation(project(":core:hiddenapi"))

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)

    // Gson
    implementation(libs.gson)

    // libsu
    implementation(libs.libsu.core)
}
