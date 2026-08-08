import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "com.a10miaomiao.bilimiao"
    compileSdk = 36

    defaultConfig {
        applicationId = "me.voltual.as"
        minSdk = 21
        targetSdk = 36
        versionCode = 117
        versionName = "2.4.8.1"

        flavorDimensions("default")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

splits {
    abi {
        isEnable = true
        reset()                          // 清空默认
        include("armeabi-v7a", "arm64-v8a")
        isUniversalApk = false           // 不生成包含所有 ABI 的万能包
    }
}
    }

    val signingFile = file("signing.properties")
    if (signingFile.exists()) {
        val props = Properties()
        props.load(FileInputStream(signingFile))
        signingConfigs {
    create("miao") {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "debug.keystore")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
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
            isMinifyEnabled = false
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
            dimension = flavorDimensionList[0]
            val channelName = project.properties["channel"] ?: "Unknown"
            manifestPlaceholders["channel"] = channelName
        }
        create("foss") {
            dimension = flavorDimensionList[0]
            manifestPlaceholders["channel"] = "FOSS"
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }


    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
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
    implementation(libs.kodein.di) // 依赖注入

    implementation(libs.kongzue.dialogx) {
        exclude("com.github.kongzue.DialogX", "DialogXInterface")
    }
    implementation(libs.materialkolor)

//    implementation("com.github.li-xiaojun:XPopup:2.9.13")
//    implementation("com.github.lihangleo2:ShadowLayout:3.2.4")

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
    
    implementation("io.github.anilbeesetti:nextlib-media3ext:1.10.1-0.13.0") // To add media3 software decoders and extensions
    implementation("io.github.anilbeesetti:nextlib-mediainfo:1.10.1-0.13.0") // To get media info through ffmpeg

    implementation(libs.okhttp3)
    implementation(libs.pbandk.runtime)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.microg.safeparcel)

    implementation(project(":bilimiao-comm"))
    implementation(project(":bilimiao-download"))
    implementation(project(":bilimiao-cover"))
//    implementation project(":bilimiao-appwidget")
    implementation(project(":bilimiao-compose"))
    // 弹幕引擎
    implementation(project(":DanmakuFlameMaster"))

    // 闭源库：百度统计、极验验证
    "fullImplementation"(libs.baidu.mobstat.sdk)
    "fullImplementation"(libs.geetest.sensebot)
    // av1解码器：https://github.com/androidx/media/tree/release/libraries/decoder_av1
    "fullImplementation"(files("libs/lib-decoder-av1-release.aar"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}