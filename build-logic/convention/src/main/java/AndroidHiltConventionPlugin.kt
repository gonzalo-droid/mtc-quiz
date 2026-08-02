import androidx.room.gradle.RoomExtension
import com.gondroid.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidHiltConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.run {
            pluginManager.run {
                apply("com.google.dagger.hilt.android")
                apply("com.google.devtools.ksp")
            }

            dependencies {
                "implementation"(libs.findLibrary("dagger.hilt.navigation.compose").get())
                "implementation"(libs.findLibrary("dagger.hilt").get())
                "ksp"(libs.findLibrary("dagger.hilt.compiler").get())
                "androidTestImplementation"(libs.findLibrary("dagger.hilt.android.testing").get())
                "kspAndroidTest"(libs.findLibrary("dagger.hilt.compiler").get())
            }
        }
    }
}