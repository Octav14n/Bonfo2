plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ksp)
}

android {
    namespace = "eu.schnuff.bonfo2"
    compileSdk = 34

    defaultConfig {
        applicationId = "eu.schnuff.bonfo2"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        multiDexEnabled = true
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(libs.jsoup)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.layout.constraintlayout)
    implementation(libs.androidx.layout.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.testRunner)
    androidTestImplementation(libs.espressoCore)

    // AppIntro
    implementation(libs.com.github.appIntro)

    // RecyclerView
    implementation(libs.androidx.layout.recyclerview)
    implementation(libs.com.simplecityapps.fastscroll)

    // Room components
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    androidTestImplementation(libs.room.testing)
    implementation(libs.room.paging)

    // Paging
    implementation(libs.androidx.paging.runtime)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Kotlin components
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)

    // Multidex
    implementation(libs.androidx.multidex)

    // Material design
    implementation(libs.com.google.android.material)

    // Preference components
    implementation(libs.androidx.preference.ktx)

    // Work service Manager
    implementation(libs.androidx.work.runtime.ktx)

    // Permission library
    implementation(libs.com.karumi.dexter)

    // Folder library
    implementation(libs.com.afollestad.materialdialog.core)
    implementation(libs.com.afollestad.materialdialog.lifecycle)
    implementation(libs.com.afollestad.materialdialog.files)
}
