// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kapt) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.com.android.library) apply false
    alias(libs.plugins.ksp) apply false
}
