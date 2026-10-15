package com.example.dbmagazynur.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Dynamic color is available on Android 12+
@Composable
actual fun kolorySystemowe(ciemny: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (ciemny) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
