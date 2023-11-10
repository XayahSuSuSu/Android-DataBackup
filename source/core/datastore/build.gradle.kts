plugins {
    alias(libs.plugins.library.common)
}

android {
    namespace = "com.xayah.core.datastore"
}

dependencies {
    // Core
    implementation(project(":core:model"))

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)
}
