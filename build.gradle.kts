// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.navigation.safeargs) apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.16" apply false
}

// 解决已弃用API警告
allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()
            freeCompilerArgs = listOf("-Xjvm-default=all")
        }
    }
}


// 设置Gradle警告模式
gradle.startParameter.showStacktrace = org.gradle.api.logging.configuration.ShowStacktrace.ALWAYS

// 添加wrapper任务
tasks.wrapper {
    gradleVersion = "8.2"
    distributionType = Wrapper.DistributionType.ALL
}

// Gradle配置
gradle.beforeProject {
    extra.apply {
        set("compileSdkVersion", 34)
        set("targetSdkVersion", 34)
        set("minSdkVersion", 24)
    }
}