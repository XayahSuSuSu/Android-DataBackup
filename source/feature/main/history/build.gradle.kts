plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
    alias(libs.plugins.library.compose)
}

android {
    namespace = "com.xayah.feature.main.history"
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:util"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    compileOnly(project(":core:hiddenapi"))
    implementation(project(":core:rootservice"))

    // Hilt navigation
    implementation(libs.androidx.hilt.navigation.compose)
}
