plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
    alias(libs.plugins.library.hilt.work)
}

android {
    namespace = "com.xayah.core.work"
}

dependencies {
    // Core
    implementation(project(":core:datastore"))
    implementation(project(":core:data"))
    implementation(project(":core:util"))
    implementation(project(":core:model"))

    // Work manager
    implementation(libs.androidx.work.runtime.ktx)
}
