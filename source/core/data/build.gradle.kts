plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
}

android {
    namespace = "com.xayah.core.data"
}

dependencies {
    // Core
    implementation(project(":core:database"))
    implementation(project(":core:util"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))

    // RootService
    implementation(project(":librootservice"))

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)
}
