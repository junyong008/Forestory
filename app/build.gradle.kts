plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.android.gms.oss-licenses-plugin")
    id("com.google.devtools.ksp")
    id("kotlin-kapt")
}

android {
    namespace = "com.yjy.forestory"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yjy.forestory"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "1.2.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    configurations.configureEach {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
}

dependencies {
    // Paging
    implementation("androidx.paging:paging-runtime-ktx:3.3.6")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Google APIs
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.api-client:google-api-client-android:1.23.0")
    implementation("com.google.api-client:google-api-client-gson:1.23.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev136-1.25.0")

    // Google Play Services
    implementation("com.google.android.gms:play-services-ads:24.1.0")
    implementation("com.android.billingclient:billing:7.1.1")
    implementation("com.google.android.gms:play-services-oss-licenses:17.1.0")

    // AndroidX
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.9")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.9")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.datastore:datastore-preferences:1.1.4")

    // 기타 라이브러리
    implementation("com.github.chrisbanes:PhotoView:2.3.0") // 이미지 확대
    implementation("de.hdodenhof:circleimageview:3.1.0") // 원형 이미지뷰
    implementation("com.github.logansdk:logan-permission:0.9.26") // 권한 요청
    implementation("com.vanniktech:android-image-cropper:4.6.0") // 사진 Crop
    implementation("io.github.muddz:styleabletoast:2.4.0") // 커스텀 토스트
    implementation("com.google.android.flexbox:flexbox:3.0.0") // Flex 레이아웃
    implementation("com.github.bumptech.glide:glide:4.15.1") // 이미지 로딩
    implementation("com.google.code.gson:gson:2.10.1") // Gson 직렬화
}
