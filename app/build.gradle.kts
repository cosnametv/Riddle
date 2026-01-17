plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.cosname.infiniteriddle"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cosname.infiniteriddle"
        minSdk = 24
        targetSdk = 35
        versionCode = 29
        versionName = "1.1.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation (libs.play.services.ads)
    implementation (libs.lottie)
    implementation (libs.gson)
    implementation("com.google.android.gms:play-services-games-v2:19.0.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.android.billingclient:billing:7.1.1")


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.ui.graphics.android)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.review)
    implementation(libs.asset.delivery)
    implementation(libs.review.v201)
    implementation(libs.app.update)
}