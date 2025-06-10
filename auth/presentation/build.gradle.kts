plugins {
    alias(libs.plugins.mtcquiz.android.feature.ui)
}

android {
    namespace = "com.gondroid.auth.presentation"
}

dependencies {

    // Librerias Android y compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.lifecycle.viewmodel.compose)


    implementation(libs.timber)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.foundation.android)

    // Image
    implementation(libs.coil.compose)

    // Lotiee
    implementation(libs.lottie.compose)

    //implementation(project(":auth:domain"))
    implementation(projects.auth.domain)
    implementation(projects.core.domain)
    implementation(projects.core.presentation.designsystem)
}