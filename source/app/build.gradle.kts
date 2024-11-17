import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    alias(libs.plugins.application.common)
    alias(libs.plugins.application.hilt)
    alias(libs.plugins.application.hilt.work)
    alias(libs.plugins.application.compose)
    alias(libs.plugins.refine)
}

android {
    namespace = "com.xayah.databackup"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.xayah.databackup"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String[]", "SUPPORTED_LOCALES", generateSupportedLocales())
    }

    lint {
        disable += "MissingTranslation"
    }

    flavorDimensions += listOf("abi", "feature")
    productFlavors {
        create("arm64-v8a") {
            dimension = "abi"
            versionCode = 4 + (android.defaultConfig.versionCode ?: 0)
            ndk.abiFilters.add("arm64-v8a")
        }
        create("armeabi-v7a") {
            dimension = "abi"
            versionCode = 3 + (android.defaultConfig.versionCode ?: 0)
            ndk.abiFilters.add("armeabi-v7a")
        }
        create("x86_64") {
            dimension = "abi"
            versionCode = 2 + (android.defaultConfig.versionCode ?: 0)
            ndk.abiFilters.add("x86_64")
        }
        create("x86") {
            dimension = "abi"
            versionCode = 1 + (android.defaultConfig.versionCode ?: 0)
            ndk.abiFilters.add("x86")
        }
        create("foss") {
            dimension = "feature"
            applicationIdSuffix = ".foss"
        }
        create("premium") {
            dimension = "feature"
            applicationIdSuffix = ".premium"
        }
        create("alpha") {
            dimension = "feature"
            applicationIdSuffix = ".alpha"
            versionCode = libs.versions.versionCodeAlpha.get().toInt()
            versionName = libs.versions.versionCodeAlpha.get()
        }
    }

    applicationVariants.all {
        outputs.forEach { output ->
            (output as BaseVariantOutputImpl).outputFileName =
                "DataBackup-${versionName}-${productFlavors[0].name}-${productFlavors[1].name}-${buildType.name}.apk"
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

fun generateSupportedLocales(): String {
    val foundLocales = StringBuilder()
    foundLocales.append("new String[]{")

    val languages = mutableListOf<String>()
    fileTree("src/main/res").visit {
        if(file.path.endsWith("strings.xml")){
            var languageCode = file.parent.replace("\\", "/").split('/').last()
                .replace("values-", "").replace("-r", "-")
            if (languageCode == "values") {
                languageCode = "en"
            }
            languages.add(languageCode)
        }
    }
    languages.sorted().forEach {
        foundLocales.append("\"").append(it).append("\"").append(",")
    }

    foundLocales.append("}")
    return foundLocales.toString().replace(",}","}")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Core
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:util"))
    implementation(project(":core:work"))
    compileOnly(project(":core:hiddenapi"))
    implementation(project(":core:rootservice"))

    // Feature
    implementation(project(":feature:crash"))
    implementation(project(":feature:setup"))
    "fossImplementation"(project(":feature:flavor:foss"))
    "premiumImplementation"(project(":feature:flavor:premium"))
    "alphaImplementation"(project(":feature:flavor:alpha"))
    "alphaImplementation"(project(":feature:flavor:foss"))
    implementation(project(":feature:main:dashboard"))
    implementation(project(":feature:main:restore"))
    implementation(project(":feature:main:cloud"))
    implementation(project(":feature:main:settings"))
    implementation(project(":feature:main:configurations"))
    implementation(project(":feature:main:processing"))
    implementation(project(":feature:main:list"))
    implementation(project(":feature:main:details"))
    implementation(project(":feature:main:history"))
    implementation(project(":feature:main:directory"))

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // libsu
    implementation(libs.libsu.core)

    // BountyCastle
    implementation(libs.bountycastle)
}
