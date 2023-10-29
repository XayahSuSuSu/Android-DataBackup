plugins {
    alias(libs.plugins.library.common)
}

android {
    namespace = "com.xayah.core.common"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}