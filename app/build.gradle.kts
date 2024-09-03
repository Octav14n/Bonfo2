import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ksp)
}

android {
    val versionPropsFile = file("version.properties")
    var versionBuild: Int
    namespace = "eu.schnuff.bonfo2"
    compileSdk = 34

    /*Setting default value for versionBuild which is the last incremented value stored in the file */
    if (versionPropsFile.canRead()) {
        val versionProps = Properties()
        versionProps.load(FileInputStream(versionPropsFile))
        versionBuild = (versionProps["VERSION_BUILD"] as String).toInt()
    } else {
        throw FileNotFoundException("Could not read version.properties!")
    }
    /*Wrapping inside a method avoids auto incrementing on every gradle task run. Now it runs only when we build apk*/
    val autoIncrementBuildNumber = fun() {

        if (versionPropsFile.canRead()) {
            val versionProps = Properties()
            versionProps.load(FileInputStream(versionPropsFile))
            versionBuild = (versionProps["VERSION_BUILD"] as String).toInt() + 1
            versionProps["VERSION_BUILD"] = versionBuild.toString()
            versionProps.store(versionPropsFile.writer(), null)
        } else {
            throw FileNotFoundException("Could not read version.properties!")
        }
    }
    // Hook to check if the release/debug task is among the tasks to be executed.
    //Let's make use of it
    gradle.taskGraph.whenReady(closureOf<TaskExecutionGraph> {
        if (this.hasTask(":app:assembleDebug")) {  /* when run debug task */
            autoIncrementBuildNumber()
        } else if (this.hasTask(":app:assembleRelease")) { /* when run release task */
            autoIncrementBuildNumber()
        }
    })

    if (hasProperty("releaseStoreFile")) {
        signingConfigs {
            create("release") {
                val releaseStoreFile: String by project
                val RELEASE_STORE_PASSWORD: String by project
                val RELEASE_KEY_ALIAS: String by project
                val RELEASE_KEY_PASSWORD: String by project

                if (!file(releaseStoreFile).exists())
                    logger.warn("Signing: Release store file does not exist.")
                if (RELEASE_STORE_PASSWORD == "")
                    logger.warn("Signing: {} is empty.", "RELEASE_STORE_PASSWORD")
                if (RELEASE_KEY_ALIAS == "")
                    logger.warn("Signing: {} is empty.", "RELEASE_KEY_ALIAS")
                if (RELEASE_KEY_PASSWORD == "")
                    logger.warn("Signing: {} is empty.", "RELEASE_KEY_PASSWORD")
                if (!file(releaseStoreFile).exists() || RELEASE_STORE_PASSWORD == "" || RELEASE_KEY_ALIAS == "" || RELEASE_KEY_PASSWORD == "")
                    throw GradleException("Signing not configured right.")

                storeFile = file(releaseStoreFile)
                storePassword = RELEASE_STORE_PASSWORD
                keyAlias = RELEASE_KEY_ALIAS
                keyPassword = RELEASE_KEY_PASSWORD

                // Optional, specify signing versions used
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
        println("Signing file found. Singing config active.")
    } else
        println("No Release file found.")

    defaultConfig {
        applicationId = "eu.schnuff.bonfo2"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "$versionCode.${"%04d".format(versionBuild)}"
        setProperty("archivesBaseName", "Bonfo2_v$versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        multiDexEnabled = true

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }

    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasProperty("releaseStoreFile"))
                signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            ndk {
                isMinifyEnabled = false
                abiFilters += listOf("x86", "x86_64")
            }
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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.layout.constraintlayout)
    implementation(libs.androidx.layout.swiperefreshlayout)
    testImplementation(libs.junit)
    testImplementation(libs.jsr305)
    androidTestImplementation(libs.testRunner)
    androidTestImplementation(libs.espressoCore)

    // AppIntro
    implementation(libs.com.github.appIntro)

    // RecyclerView
    implementation(libs.androidx.layout.recyclerview)
    implementation(libs.me.zhanghai.android.fastscroll)

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
