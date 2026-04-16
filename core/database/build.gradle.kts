plugins {
    alias(libs.plugins.mtcquiz.android.library)
    alias(libs.plugins.mtcquiz.android.hilt)
    alias(libs.plugins.mtcquiz.android.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.gondroid.core.database"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(projects.core.domain)
}