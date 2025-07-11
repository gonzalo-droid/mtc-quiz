plugins {
    alias(libs.plugins.mtcquiz.android.feature.ui)
    alias(libs.plugins.mtcquiz.android.hilt)
}
android {
    namespace = "com.gondroid.configuration.presentation"
}

dependencies {

    // Librerias Android y compose
    implementation(libs.timber)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.text.google.fonts)

    // Image
    implementation(libs.coil.compose)

    // Lotiee
    implementation(libs.lottie.compose)

    implementation(projects.configuration.domain)
    implementation(projects.core.domain)
    implementation(projects.core.presentation.designsystem)
}