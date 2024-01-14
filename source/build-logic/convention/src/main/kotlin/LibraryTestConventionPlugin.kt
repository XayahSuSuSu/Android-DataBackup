import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class LibraryTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.getByType<LibraryExtension>().apply {
                dependencies {
                    add("testImplementation", catalogLibs.findLibrary("junit").get())
                }
            }
        }
    }
}
