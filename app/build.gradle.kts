// Aplikacja na Androida. Ekrany i logika są w module :shared (wspólne z programem na Windows),
// tu zostaje tylko to, co androidowe: MainActivity, manifest, ikona i aparat (FileProvider).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.dbmagazynur"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.dbmagazynur"
        minSdk = 29
        targetSdk = 37
        // Wersja wspólna z programem na Windows (gradle.properties)
        versionCode = providers.gradleProperty("magazyn.kodWersji").get().toInt()
        versionName = providers.gradleProperty("magazyn.wersja").get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
