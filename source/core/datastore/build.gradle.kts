plugins {
    alias(libs.plugins.library.common)
}

android {
    namespace = "com.xayah.core.datastore"
}

dependencies {
    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)
}
