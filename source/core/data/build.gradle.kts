plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
    alias(libs.plugins.library.protobuf)
    alias(libs.plugins.library.androidTest)
    alias(libs.plugins.library.compose)
    alias(libs.plugins.refine)
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
    compileOnly(project(":core:hiddenapi"))

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)

    // Gson
    implementation(libs.gson)

    // libsu
    implementation(libs.libsu.core)

    // PickYou
    implementation(libs.pickyou)

    // Work manager
    implementation(libs.androidx.work.runtime.ktx)
}
