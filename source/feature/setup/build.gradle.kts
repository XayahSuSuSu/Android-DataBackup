plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
    alias(libs.plugins.library.compose)
}

android {
    namespace = "com.xayah.feature.setup"
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:util"))
    implementation(project(":core:datastore"))
    implementation(project(":core:data"))
    implementation(project(":core:work"))
    implementation(project(":feature:main:directory"))
    implementation(project(":feature:main:configurations"))

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)

    // libsu
    implementation(libs.libsu.core)

    // PickYou
    implementation(libs.pickyou)
}