plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.compose)
}

android {
    namespace = "com.xayah.core.common"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}