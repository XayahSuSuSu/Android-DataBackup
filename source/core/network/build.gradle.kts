plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
}

android {
    namespace = "com.xayah.core.network"
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:util"))
    implementation(project(":core:database"))
    implementation(project(":core:model"))

    // OkHttp
    implementation(libs.okhttp)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Gson
    implementation(libs.gson)

    // Backends
    implementation(libs.apache.commons.net)
    implementation(libs.smbj)
    implementation(libs.guava.compat)
    implementation(libs.smbj.rpc) {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    }
    implementation(libs.sardine.android)

    // PickYou
    implementation(libs.pickyou)
}