import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// Wspólny kod aplikacji (Compose Multiplatform) dla Androida i Windows,
// a w części desktop także sam program na Windows.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Adres projektu i klucz publishable Supabase trzymamy w local.properties (poza gitem)
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProperty(name: String): String =
    localProperties.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw GradleException(
            "Brak $name w local.properties. Wpisz adres projektu (SUPABASE_URL) " +
                "i klucz publishable (SUPABASE_KEY) z panelu Supabase."
        )

val wersja = providers.gradleProperty("magazyn.wersja").get()
val kodWersji = providers.gradleProperty("magazyn.kodWersji").get()

// Odpowiednik androidowego BuildConfig, dostępny też na Windows
val generujKonfiguracje = tasks.register("generujKonfiguracje") {
    val pola = mapOf(
        "SUPABASE_URL" to localProperty("SUPABASE_URL"),
        "SUPABASE_KEY" to localProperty("SUPABASE_KEY"),
        "WERSJA" to wersja,
        "KOD_WERSJI" to kodWersji,
    )
    val katalog = layout.buildDirectory.dir("generated/konfiguracja")
    inputs.properties(pola)
    outputs.dir(katalog)
    doLast {
        fun tekst(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""
        val plik = katalog.get().file("com/example/dbmagazynur/Konfiguracja.kt").asFile
        plik.parentFile.mkdirs()
        plik.writeText(
            buildString {
                appendLine("// Plik generowany przez shared/build.gradle.kts, nie edytuj")
                appendLine("package com.example.dbmagazynur")
                appendLine()
                appendLine("internal object Konfiguracja {")
                pola.forEach { (nazwa, wartosc) -> appendLine("    const val $nazwa = ${tekst(wartosc)}") }
                appendLine("}")
            },
        )
    }
}

kotlin {
    android {
        namespace = "com.example.dbmagazynur.shared"
        compileSdk = 37
        minSdk = 29
        // Zasoby Compose (ikony, katalog, teksty „O aplikacji”) także na Androidzie
        androidResources { enable = true }
    }

    jvm("desktop")

    sourceSets {
        commonMain {
            kotlin.srcDir(generujKonfiguracje)
            dependencies {
                api(libs.cmp.runtime)
                implementation(libs.cmp.foundation)
                implementation(libs.cmp.ui)
                implementation(libs.cmp.material3)
                implementation(libs.cmp.resources)
                implementation(libs.cmp.preview)
                implementation(libs.lifecycle.viewmodel.compose)
                implementation(project.dependencies.platform(libs.supabase.bom))
                implementation(libs.supabase.auth)
                implementation(libs.supabase.postgrest)
                implementation(libs.supabase.storage)
                implementation(libs.supabase.coil)
                implementation(libs.supabase.functions)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
                implementation(libs.multiplatform.settings)
            }
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
        }
        getByName("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.okhttp)
            }
        }
        getByName("desktopTest") {
            dependencies {
                implementation(libs.junit)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.example.dbmagazynur.zasoby"
    publicResClass = false
}

// Program na Windows: uruchomienie `:shared:run`, instalator `:shared:packageMsi`
compose.desktop {
    application {
        mainClass = "com.example.dbmagazynur.MainKt"
        // JBR z Android Studio nie ma jpackage, więc program pakuje pełny JDK (Temurin),
        // który Gradle pobiera sam przy pierwszym użyciu
        javaHome = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(25)
            vendor = JvmVendorSpec.ADOPTIUM
        }.get().metadata.installationPath.asFile.absolutePath
        // Silnik graficzny Compose ładuje bibliotekę natywną; bez tego Java 25 ostrzega przy starcie
        jvmArgs += "--enable-native-access=ALL-UNNAMED"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            // Moduły Javy dołączane do programu (wynik :shared:suggestRuntimeModules).
            // jdk.localedata ręcznie: polski format liczb i sortowanie (analiza go nie wykrywa).
            modules("java.instrument", "java.management", "java.prefs", "jdk.unsupported", "jdk.localedata")
            packageName = "Magazyn UR"
            packageVersion = wersja
            description = "Magazyn UR"
            windows {
                menu = true
                shortcut = true
                menuGroup = "Magazyn UR"
                // Stały identyfikator: nowa wersja instalatora aktualizuje starą zamiast instalować drugą
                upgradeUuid = "6c0a7a3e-5d1b-4b8e-9b7a-2f3c4d5e6f70"
            }
        }
    }
}
