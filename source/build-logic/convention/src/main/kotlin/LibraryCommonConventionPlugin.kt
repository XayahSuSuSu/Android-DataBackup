import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val Project.catalogLibs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

private fun Project.configureCommon() {
    pluginManager.apply("com.android.library")
    pluginManager.apply("org.jetbrains.kotlin.android")

    extensions.getByType<LibraryExtension>().apply {
        compileSdk = catalogLibs.findVersion("compileSdk").get().toString().toInt()

        defaultConfig {
            minSdk = catalogLibs.findVersion("minSdk").get().toString().toInt()
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs.add("-Xcontext-receivers")
            }
        }
    }
}

class LibraryCommonConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureCommon()
        }
    }
}
