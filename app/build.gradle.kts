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
    implementation(libs.appcompat)
    implementation(libs.coreKtx)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.testRunner)
    androidTestImplementation(libs.espressoCore)

    // AppIntro
    implementation(libs.appIntro)

    // RecyclerView
    implementation(libs.recyclerview)
    implementation(libs.fastscroll)

    // Room components
    implementation(libs.roomRuntime)
    ksp(libs.roomCompiler)
    androidTestImplementation(libs.roomTesting)
    implementation(libs.roomPaging)

    // Paging
    implementation(libs.pagingRuntime)

    // Lifecycle components
    implementation(libs.lifecycleExtensions)
    implementation(libs.lifecycleViewmodelKtx)

    // Kotlin components
    api(libs.coroutinesCore)
    api(libs.coroutinesAndroid)

    // Multidex
    implementation(libs.multidex)

    // Material design
    implementation(libs.material)

    // Preference components
    implementation(libs.preferenceKtx)

    // Work service Manager
    implementation(libs.workRuntimeKtx)

    // Permission library
    implementation(libs.dexter)

    // Folder library
    implementation(libs.folderCore)
    implementation(libs.folderLifecycle)
    implementation(libs.folderFiles)
}
