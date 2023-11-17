plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
}

android {
    namespace = "com.xayah.core.network"
}

dependencies {
    // OkHttp
    implementation(libs.okhttp)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Gson
    implementation(libs.gson)
}