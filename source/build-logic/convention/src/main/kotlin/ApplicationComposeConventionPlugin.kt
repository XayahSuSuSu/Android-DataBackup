import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class ApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.getByType<ApplicationExtension>().apply {
                defaultConfig {
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                }
                buildFeatures {
                    compose = true
                }

                dependencies {
                    add("implementation", catalogLibs.findLibrary("androidx.core.ktx").get())
                    add("implementation", catalogLibs.findLibrary("androidx.appcompat").get())
                    add("implementation", catalogLibs.findLibrary("androidx.lifecycle.runtime.ktx").get())
                    add("implementation", catalogLibs.findLibrary("androidx.lifecycle.runtime.compose").get())
                    add("implementation", catalogLibs.findLibrary("androidx.activity.compose").get())
                    add("implementation", platform(catalogLibs.findLibrary("androidx-compose-bom").get()))
                    add("implementation", catalogLibs.findLibrary("androidx.compose.ui").get())
                    add("implementation", catalogLibs.findLibrary("androidx.compose.ui.graphics").get())
                    add("debugImplementation", catalogLibs.findLibrary("androidx.compose.ui.tooling.preview").get())
                    add("debugImplementation", catalogLibs.findLibrary("androidx.compose.ui.tooling").get())
                    add("implementation", catalogLibs.findLibrary("androidx.compose.material3").get())
                    add("implementation", catalogLibs.findLibrary("androidx.compose.material.icons.extended").get())
                }
            }
        }
    }
}
