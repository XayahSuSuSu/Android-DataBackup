import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class LibraryAndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.getByType<LibraryExtension>().apply {
                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                dependencies {
                    add("androidTestImplementation", catalogLibs.findLibrary("androidx.test.ext.junit").get())
                    add("androidTestImplementation", catalogLibs.findLibrary("espresso.core").get())
                }
            }
        }
    }
}
