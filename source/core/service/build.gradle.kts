plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
    alias(libs.plugins.library.protobuf)
}

android {
    namespace = "com.xayah.core.service"
}

dependencies {
    // Core
    implementation(project(":core:model"))
    implementation(project(":core:util"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))

    // RootService
    implementation(project(":librootservice"))
}