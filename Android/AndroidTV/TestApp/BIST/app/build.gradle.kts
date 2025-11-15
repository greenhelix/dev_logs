plugins {
    alias(libs.plugins.android.application)
}


android {
    namespace = "com.innopia.bist.ver2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.innopia.bist.ver2"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

    }

    flavorDimensions += "version"

    productFlavors {
        create("ver1") {
            dimension = "version"
            // ver1 전용 설정을 추가할 수 있습니다 (예: applicationIdSuffix ".ver1")
        }
        create ("ver2") {
            dimension = "version"
        }
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
// Leanback 라이브러리 (필수)
    implementation("androidx.leanback:leanback:1.0.0")
// CardView
    implementation("androidx.cardview:cardview:1.0.0")
// Glide (이미지 로딩)
    implementation("com.github.bumptech.glide:glide:4.15.1")
// RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.0")

    // MPAndroidChart
    implementation (libs.philjay.mpandroidchart)

    // Glide (이미지 로딩)
    implementation ("com.github.bumptech.glide:glide:5.0.5")
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")
}