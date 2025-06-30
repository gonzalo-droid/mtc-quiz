plugins {
    alias(libs.plugins.mtcquiz.android.library.compose)
}

android {
    namespace = "com.gondroid.core.presentation.ui"
}

dependencies {
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)

    implementation(projects.core.domain)
    implementation(projects.core.presentation.designsystem)

}