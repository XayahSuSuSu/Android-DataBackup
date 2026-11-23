import com.android.build.api.variant.BuildConfigField

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.xayah.libnative"
    ndkVersion = libs.versions.ndkVersion.get()
    compileSdk = libs.versions.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            java.directories += "src/main/jni/external/zstd/zstd-jni/src/main/java"
        }
    }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                arguments.addAll(listOf("-DANDROID_PLATFORM=24"))
            }
        }

        // Optional comma-separated ABI list (for example -Pdatabackup.abis=arm64-v8a) to shorten native builds.
        providers.gradleProperty("databackup.abis").orNull?.let { abis ->
            ndk {
                abiFilters += abis.split(",").map(String::trim).filter(String::isNotEmpty)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/jni/CMakeLists.txt")
            version = libs.versions.cmakeVersion.get()
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

val zstdVersion = file("src/main/jni/external/zstd/zstd-jni/version").readText().trim()

androidComponents {
    onVariants { variant ->
        variant.buildConfigFields?.put(
            "ZSTD_VERSION",
            BuildConfigField(
                type = "String",
                value = "\"$zstdVersion\"",
                comment = "Zstandard version",
            ),
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
