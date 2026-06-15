import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "edu.touro.habitcoach"
    compileSdk = 36

    defaultConfig {
        applicationId = "edu.touro.habitcoach"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load secrets from secrets.properties
        val secrets = Properties()
        val secretsFile = rootProject.file("secrets.properties")
        if (secretsFile.exists()) {
            secretsFile.inputStream().use { stream ->
                secrets.load(stream)
            }
        }

        // Safely inject secrets into BuildConfig.
        buildConfigField("String", "GITHUB_TOKEN", "\"${secrets.getProperty("GITHUB_TOKEN", "")}\"")
        buildConfigField("String", "SEARCH_ENDPOINT", "\"${secrets.getProperty("SEARCH_ENDPOINT", "")}\"")
        buildConfigField("String", "SEARCH_KEY", "\"${secrets.getProperty("SEARCH_KEY", "")}\"")
        buildConfigField("String", "INDEX_NAME", "\"${secrets.getProperty("INDEX_NAME", "")}\"")
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.konfetti)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.azure:azure-ai-inference:1.0.0-beta.5")
    implementation("io.projectreactor:reactor-core:3.6.0")
    implementation("com.azure:azure-search-documents:11.7.0")
}
