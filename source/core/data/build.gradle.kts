plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
}

android {
    namespace = "com.xayah.core.data"
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:util"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(project(":core:ui"))

    // RootService
    implementation(project(":librootservice"))

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)
}
