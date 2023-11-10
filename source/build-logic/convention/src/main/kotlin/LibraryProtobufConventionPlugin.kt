import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class LibraryProtobufConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            extensions.getByType<LibraryExtension>().apply {
                dependencies {
                    add("implementation", catalogLibs.findLibrary("serialization.protobuf").get())
                }
            }
        }
    }
}
