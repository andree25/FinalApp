import com.android.build.api.dsl.Packaging

plugins {
    alias(libs.plugins.androidApplication)
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.app.finalapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.app.finalapp"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appScheme"] = "com.app.finalapp.braintree"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        viewBinding = true
    }
    packagingOptions {
        resources {
            excludes += setOf(
                    "META-INF/DEPENDENCIES",
                    "META-INF/LICENSE",
                    "META-INF/LICENSE.txt",
                    "META-INF/license.txt",
                    "META-INF/NOTICE",
                    "META-INF/NOTICE.txt",
                    "META-INF/notice.txt",
                    "META-INF/ASL2.0"
            )
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.legacy.support.v4)
    implementation(libs.places)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform(libs.firebase.bom))
    //noinspection UseTomlInstead
    implementation("com.google.firebase:firebase-analytics")
    implementation (libs.firebase.auth)
    implementation (libs.firebase.storage)
    implementation (libs.firebase.database)
    implementation (libs.firebase.functions)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
    implementation (libs.play.services.wallet)
    implementation (libs.braintree)
    implementation (libs.okhttp)
    implementation (libs.cardview)
    implementation (libs.play.services.maps)
    implementation (libs.places)
    implementation (libs.converter.gson)
    implementation (libs.okhttp.v491)
    implementation(libs.retrofit)
    implementation(libs.google.api.client)
    implementation(libs.google.oauth.client.jetty)
    implementation(libs.google.http.client.gson)
    implementation(libs.google.api.services.blogger.vv3rev202206251321)
    implementation(libs.recyclerview)
    implementation(libs.picasso)
}