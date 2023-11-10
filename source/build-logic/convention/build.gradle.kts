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
}

gradlePlugin {
    plugins {
        register("libraryCommon") {
            id = "library.common"
            implementationClass = "LibraryCommonConventionPlugin"
        }
        register("libraryHilt") {
            id = "library.hilt"
            implementationClass = "LibraryHiltConventionPlugin"
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
    }
}
