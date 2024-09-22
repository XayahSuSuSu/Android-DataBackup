plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
    alias(libs.plugins.library.compose)
    alias(libs.plugins.refine)
}

android {
    namespace = "com.xayah.feature.main.processing"
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:datastore"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:util"))
    implementation(project(":core:rootservice"))
    compileOnly(project(":core:hiddenapi"))
    implementation(project(":core:service"))
    implementation(project(":core:network"))

    // Hilt navigation
    implementation(libs.androidx.hilt.navigation.compose)

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)

    // dotLottie
    implementation(libs.dotlottie.android)

    // Vector animation
    implementation(libs.androidx.animation.graphics)
}
