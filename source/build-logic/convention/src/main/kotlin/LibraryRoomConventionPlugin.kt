import com.android.build.gradle.LibraryExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class LibraryRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")

            extensions.getByType<LibraryExtension>().apply {
                dependencies {
                    add("implementation", catalogLibs.findLibrary("androidx.room.runtime").get())
                    add("implementation", catalogLibs.findLibrary("androidx.room.ktx").get())
                    add("ksp", catalogLibs.findLibrary("androidx.room.compiler").get())
                }
            }

            extensions.configure<KspExtension> {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }
}
