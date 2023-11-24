plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.protobuf)
}

android {
    namespace = "com.xayah.core.rootservice"

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        aidl = true
    }
}

dependencies {
    implementation(project(":core:hiddenapi"))
    implementation(project(":core:util"))

    implementation(libs.kotlinx.coroutines.core.jvm)

    // libsu
    implementation(libs.libsu.service)
}