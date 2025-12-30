plugins {
    alias(libs.plugins.mtcquiz.android.application.compose)
    alias(libs.plugins.mtcquiz.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.com.google.gms.google.services)
    alias(libs.plugins.com.google.firebase.crashlytics)
}

android {

    namespace = "com.gondroid.mtcquiz"


    signingConfigs {
        create("release") {
            storeFile = file(
                System.getenv("MTC_KEYSTORE_PATH")
                    ?: project.findProperty("MTC_KEYSTORE_PATH") as String
            )
            storePassword = System.getenv("MTC_KEYSTORE_PASSWORD")
                ?: project.findProperty("MTC_KEYSTORE_PASSWORD") as String
            keyAlias =
                System.getenv("MTC_KEY_ALIAS") ?: project.findProperty("MTC_KEY_ALIAS") as String
            keyPassword = System.getenv("MTC_KEY_PASSWORD")
                ?: project.findProperty("MTC_KEY_PASSWORD") as String
        }
    }
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.CustomTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {

    // Librerias Android y compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Tools
    implementation(libs.timber)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.ui.text.google.fonts)

    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Image
    // implementation(libs.coil.compose)

    // Lotiee
    // implementation(libs.lottie.compose)

    // Librerias Room
    // implementation(libs.room.ktx)
    // implementation(libs.room.runtime)
    // ksp(libs.room.compiler)

    // Librerias Dagger Hilt
    // implementation(libs.dagger.hilt.navigation.compose)
    // implementation(libs.dagger.hilt)
    // ksp(libs.dagger.hilt.compiler)

    // Libreria Serializacion
    implementation(libs.kotlinx.serialization.json)

    // Moshi
    // implementation(libs.moshi)
    // ksp(libs.moshi.codegen)
    // implementation(libs.moshi.kotlin)

    // Retrofit
    // implementation(libs.retrofit)
    // implementation(libs.retrofit.moshi)

    // OkHttp
    // implementation(libs.okhttp)
    // implementation(libs.okhttp.logging)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)


    // Also add the dependencies for the Credential Manager libraries and specify their versions
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    //Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)

    //Android Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.mockwebserver)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.navigation.testing)


    // Hilt Testing
    androidTestImplementation(libs.dagger.hilt.android.testing)
    kspAndroidTest(libs.dagger.hilt.compiler)

    // Debug dependencies
    testImplementation(kotlin("test"))

    implementation(projects.core.presentation.designsystem)
    implementation(projects.core.presentation.ui)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.database)

    implementation(projects.auth.presentation)
    implementation(projects.auth.domain)
    implementation(projects.auth.data)

    implementation(projects.configuration.presentation)
    implementation(projects.configuration.domain)
    implementation(projects.configuration.data)

    implementation(projects.home.presentation)
    implementation(projects.home.domain)
    implementation(projects.home.data)

    implementation(projects.detail.presentation)
    implementation(projects.detail.domain)
    implementation(projects.detail.data)

    implementation(projects.pdf.presentation)
    implementation(projects.pdf.domain)
    implementation(projects.pdf.data)

    implementation(projects.evaluation.presentation)
    implementation(projects.evaluation.domain)
    implementation(projects.evaluation.data)


    implementation(projects.questionreview.presentation)
    implementation(projects.questionreview.domain)
    implementation(projects.questionreview.data)
}