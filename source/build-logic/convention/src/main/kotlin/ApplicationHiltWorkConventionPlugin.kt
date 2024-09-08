import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class ApplicationHiltWorkConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.getByType<ApplicationExtension>().apply {
                dependencies {
                    add("implementation", catalogLibs.findLibrary("hilt.work").get())
                    add("kapt", catalogLibs.findLibrary("hilt.work.compiler").get())
                }
            }
        }
    }
}
