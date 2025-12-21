plugins {
    alias(libs.plugins.mtcquiz.android.library)
    alias(libs.plugins.mtcquiz.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.gondroid.core.data"
}

dependencies {

    implementation(libs.kotlinx.serialization.json)


    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Also add the dependencies for the Credential Manager libraries and specify their versions
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(projects.core.domain)
    implementation(projects.core.database)

}