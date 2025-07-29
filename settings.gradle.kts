// 根项目设置，用于管理插件和依赖解析

// 插件管理：定义从何处获取 Gradle 插件
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.google.dagger.hilt.android" -> useModule("com.google.dagger:hilt-android-gradle-plugin:2.50")
            }
        }
    }
}

// 依赖解析管理：定义如何解析项目依赖
dependencyResolutionManagement {
    // 强制项目仅使用在 settings.gradle.kts 中定义的仓库
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }
    }
}

// 项目根名称
rootProject.name = "ItemManagement"

// 包含的子模块
include(":app") 

// 本地AAChartCore-Kotlin模块
include(":charts")
project(":charts").projectDir = file("libs/AAChartCore-Kotlin-7.4.0/charts") 