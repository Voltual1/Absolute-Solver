import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }

    namespace = "com.a10miaomiao.bilimiao"
    compileSdk = 36

    defaultConfig {
        applicationId = "me.voltual.as"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        flavorDimensions += "default"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        resValues = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    signingConfigs {
        create("miao") {
            storeFile = file(
                System.getenv("KEYSTORE_PATH")
                    ?: keystoreProperties.getProperty("storeFile")
                    ?: "debug.keystore"
            )
            storePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: keystoreProperties.getProperty("storePassword")
            keyAlias = System.getenv("KEY_ALIAS")
                ?: keystoreProperties.getProperty("keyAlias")
            keyPassword = System.getenv("KEY_PASSWORD")
                ?: keystoreProperties.getProperty("keyPassword")
        }
    }

    packaging {
        resources {
            excludes.add("/google/protobuf/**")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "Absolute-Solver-Dev")
            manifestPlaceholders["channel"] = "Development"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.asMap["miao"]?.let {
                signingConfig = it
            }
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }

    productFlavors {
        create("full") {
            dimension = "default"
            val channelName = project.properties["channel"] ?: "Unknown"
            manifestPlaceholders["channel"] = channelName
        }
        create("foss") {
            dimension = "default"
            manifestPlaceholders["channel"] = "FOSS"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.media)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.profileinstaller)

    implementation(libs.kotlinx.coroutines.android)
    //kodein已迁移为koin
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    implementation(libs.kongzue.dialogx) {
        exclude("com.github.kongzue.DialogX", "DialogXInterface")
    }
    implementation(libs.materialkolor)

    implementation(libs.splitties.android.base)
    implementation(libs.splitties.android.base.with.views.dsl)
    implementation(libs.splitties.android.appcompat)
    implementation(libs.splitties.android.appcompat.with.views.dsl)
    implementation(libs.splitties.android.material.components)
    implementation(libs.splitties.android.material.components.with.views.dsl)

    implementation(libs.mojito)
    implementation(libs.mojito.sketch)
    implementation(libs.mojito.glide)

    // 播放器相关
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.decoder)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.gsy.video.player)

    implementation("io.github.anilbeesetti:nextlib-media3ext:1.10.1-0.13.0")
    implementation("io.github.anilbeesetti:nextlib-mediainfo:1.10.1-0.13.0")

    implementation(libs.okhttp3)
    implementation(libs.pbandk.runtime)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.microg.safeparcel)

    implementation(project(":bilimiao-comm"))
    implementation(project(":bilimiao-download"))
    implementation(project(":bilimiao-cover"))
    implementation(project(":bilimiao-compose"))
    implementation(project(":DanmakuFlameMaster"))

    "fullImplementation"(libs.baidu.mobstat.sdk)
    "fullImplementation"(libs.geetest.sensebot)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}