// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    // AGP 9 ma wbudowanego Kotlina w wersji 2.2.10, a supabase-kt wymaga 2.4.
    // Ta linia podnosi tylko wersję Kotlina, nie włączaj tej wtyczki w modułach.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}
