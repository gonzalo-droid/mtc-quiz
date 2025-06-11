plugins {
    alias(libs.plugins.mtcquiz.android.library)
    alias(libs.plugins.mtcquiz.android.hilt)
    alias(libs.plugins.mtcquiz.android.room)
}

android {
    namespace = "com.gondroid.core.database"
}

dependencies {
    implementation(projects.core.domain)
}