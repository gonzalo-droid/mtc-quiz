// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.dagger.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.com.google.gms.google.services) apply false
    alias(libs.plugins.com.google.firebase.crashlytics) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kotlinx.kover) apply false
}

allprojects {
    repositories {
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    // Kover needs to be applied to every module that should contribute to the
    // merged coverage report (not just the aggregating module), so it can
    // expose its coverage data as a consumable variant. See app/build.gradle.kts
    // for the aggregation (`dependencies { kover(project(...)) }`) and report tasks.
    apply(plugin = "org.jetbrains.kotlinx.kover")
}