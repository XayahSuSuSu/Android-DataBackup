plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
    alias(libs.plugins.library.compose)
    alias(libs.plugins.library.firebase)
}

android {
    namespace = "com.xayah.feature.flavor.premium"
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:util"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
    implementation(project(":core:provider"))

    // Common
    implementation(project(":feature:guide:common"))

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)

    // libsu
    implementation(libs.libsu.core)
}
