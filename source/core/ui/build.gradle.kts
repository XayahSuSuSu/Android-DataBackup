plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.compose)
    alias(libs.plugins.library.hilt)
}

android {
    namespace = "com.xayah.core.ui"
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:util"))
    implementation(project(":core:datastore"))

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Accompanist
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.accompanist.placeholder)
}
