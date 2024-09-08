import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.xayah.databackup.buildlogic"

// Configure the build-logic plugins to target JDK 17
// This matches the JDK used to build the project, and is not related to what is running on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    compileOnly(libs.android.build.logic)
    compileOnly(libs.kotlin.build.logic)
    compileOnly(libs.ksp.build.logic)
    compileOnly(libs.firebase.crashlytics.build.logic)
}

gradlePlugin {
    plugins {
        register("applicationCommon") {
            id = "application.common"
            implementationClass = "ApplicationCommonConventionPlugin"
        }
        register("applicationCompose") {
            id = "application.compose"
            implementationClass = "ApplicationComposeConventionPlugin"
        }
        register("applicationHilt") {
            id = "application.hilt"
            implementationClass = "ApplicationHiltConventionPlugin"
        }
        register("applicationHiltWork") {
            id = "application.hilt.work"
            implementationClass = "ApplicationHiltWorkConventionPlugin"
        }

        register("libraryCommon") {
            id = "library.common"
            implementationClass = "LibraryCommonConventionPlugin"
        }
        register("libraryHilt") {
            id = "library.hilt"
            implementationClass = "LibraryHiltConventionPlugin"
        }
        register("libraryHiltWork") {
            id = "library.hilt.work"
            implementationClass = "LibraryHiltWorkConventionPlugin"
        }
        register("libraryRoom") {
            id = "library.room"
            implementationClass = "LibraryRoomConventionPlugin"
        }
        register("libraryCompose") {
            id = "library.compose"
            implementationClass = "LibraryComposeConventionPlugin"
        }
        register("libraryProtobuf") {
            id = "library.protobuf"
            implementationClass = "LibraryProtobufConventionPlugin"
        }
        register("libraryTest") {
            id = "library.test"
            implementationClass = "LibraryTestConventionPlugin"
        }
        register("libraryAndroidTest") {
            id = "library.androidTest"
            implementationClass = "LibraryAndroidTestConventionPlugin"
        }
        register("libraryFirebase") {
            id = "library.firebase"
            implementationClass = "LibraryFirebaseConventionPlugin"
        }
    }
}
