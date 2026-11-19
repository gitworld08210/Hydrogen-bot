import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.hydrogen.bot"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hydrogen.bot"
        minSdk = 26
        targetSdk = 34
        versionCode = 13
        versionName = "2.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        val geminiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
        val supabaseUrl = localProperties.getProperty("SUPABASE_URL") ?: "https://your-project.supabase.co"
        val supabaseKey = localProperties.getProperty("SUPABASE_ANON_KEY") ?: "your-anon-key"
        val resendKey = localProperties.getProperty("RESEND_API_KEY") ?: ""
        val hermesUrl = localProperties.getProperty("HERMES_AGENT_URL") ?: ""
        val hermesKey = localProperties.getProperty("HERMES_AGENT_KEY") ?: ""
        val elevenLabsKey = localProperties.getProperty("ELEVENLABS_API_KEY") ?: ""
        val elevenLabsVoiceId = localProperties.getProperty("ELEVENLABS_VOICE_ID") ?: "21m00Tcm4TlvDq8ikWAM"

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseKey\"")
        buildConfigField("String", "RESEND_API_KEY", "\"$resendKey\"")
        buildConfigField("String", "HERMES_AGENT_URL", "\"$hermesUrl\"")
        buildConfigField("String", "HERMES_AGENT_KEY", "\"$hermesKey\"")
        buildConfigField("String", "ELEVENLABS_API_KEY", "\"$elevenLabsKey\"")
        buildConfigField("String", "ELEVENLABS_VOICE_ID", "\"$elevenLabsVoiceId\"")
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
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Supabase REST & Networking
    implementation(libs.okhttp)

    // Firebase Cloud Messaging (FCM Push Notifications)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    // Biometrics & Security Keystore (AES-256)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)

    // Gemini AI Client & Coroutines
    implementation(libs.google.generativeai)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
}
