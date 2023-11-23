import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.plugin.KaptExtension

class ApplicationHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.kapt")
            pluginManager.apply("com.google.dagger.hilt.android")

            extensions.getByType<ApplicationExtension>().apply {
                dependencies {
                    add("implementation", catalogLibs.findLibrary("hilt.android").get())
                    add("kapt", catalogLibs.findLibrary("hilt.android.compiler").get())
                }
            }

            extensions.configure<KaptExtension> {
                correctErrorTypes = true
            }
        }
    }
}
