plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.com.google.gms.google.services)
    alias(libs.plugins.com.google.firebase.crashlytics)
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.gondroid.mtcquiz"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("MTC_KEYSTORE_PATH") ?: project.findProperty("MTC_KEYSTORE_PATH") as String)
            storePassword = System.getenv("MTC_KEYSTORE_PASSWORD") ?: project.findProperty("MTC_KEYSTORE_PASSWORD") as String
            keyAlias = System.getenv("MTC_KEY_ALIAS") ?: project.findProperty("MTC_KEY_ALIAS") as String
            keyPassword = System.getenv("MTC_KEY_PASSWORD") ?: project.findProperty("MTC_KEY_PASSWORD") as String
        }
    }

    defaultConfig {
        applicationId = "com.gondroid.mtcquiz"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        // keytool -list -v -keystore /Users/gonzalo/AndroidStudioProjects/keys/mtcquizkeys -alias mtcquizkeys -> hash firebase
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            versionNameSuffix = ".debug"
        }
    }

    flavorDimensions += listOf("environment")
    productFlavors {
        create("qa") {
            dimension = "environment"
            applicationId = "com.gondroid.mtcquiz"
        }
        create("prod") {
            dimension = "environment"
            applicationId = "com.gondroid.mtcquiz"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
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
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.timber)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.foundation.android)
    debugImplementation(libs.androidx.ui.tooling)

    // Image
    implementation(libs.coil.compose)

    // Lotiee
    implementation(libs.lottie.compose)
    // Librerias Room
    implementation(libs.room.ktx)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // Librerias Dagger Hilt
    implementation(libs.dagger.hilt.navigation.compose)
    implementation(libs.dagger.hilt)
    ksp(libs.dagger.hilt.compiler)

    // Libreria Serializacion
    implementation(libs.kotlinx.serialization.json)

    // Moshi
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)
    implementation(libs.moshi.kotlin)
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

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


    // Test dependencies
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // Android Test dependencies
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.mockk)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.navigation.testing)

    // Debug dependencies
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}