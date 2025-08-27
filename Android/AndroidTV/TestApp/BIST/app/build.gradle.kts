plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.innopia.bist"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.innopia.bist"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
    implementation ("androidx.media3:media3-exoplayer:1.3.1")
    implementation ("androidx.media3:media3-ui:1.3.1")
//    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
//    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
//    implementation("com.google.android.exoplayer:exoplayer-common:2.19.1")
//    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.appcompat)
}