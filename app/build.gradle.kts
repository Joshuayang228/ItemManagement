plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // id("kotlin-kapt")
    alias(libs.plugins.navigation.safeargs)
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.itemmanagement"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.itemmanagement"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 添加Room模式导出目录
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    
    androidResources {
        additionalParameters += listOf("--warn-manifest-validation")
    }
    
    lint {
        // 添加lint配置
        abortOnError = false
        checkReleaseBuilds = false
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    // AndroidX核心库
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle组件
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)

    // Navigation组件
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Room数据库
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    // kapt(libs.room.compiler)
    ksp(libs.room.compiler)

    // Glide图片加载
    implementation(libs.glide)
    // kapt(libs.glide.compiler)
    ksp(libs.glide.compiler)

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)

    // Google Flexbox
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    
    // AAInfographics (AAChartCore-Kotlin) for charts - 本地依赖
    implementation(project(":charts"))
} 