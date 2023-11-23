import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class ApplicationFirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.gms.google-services")
            pluginManager.apply("com.google.firebase.crashlytics")

            extensions.getByType<ApplicationExtension>().apply {
                dependencies {
                    // Firebase
                    add("implementation", platform(catalogLibs.findLibrary("firebase-bom").get()))
                    add("implementation", catalogLibs.findLibrary("firebase.crashlytics").get())
                    add("implementation", catalogLibs.findLibrary("firebase.analytics").get())
                }
            }
        }
    }
}
