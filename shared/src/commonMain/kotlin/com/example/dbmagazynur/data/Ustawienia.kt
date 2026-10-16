package com.example.dbmagazynur.data

import com.russhwolf.settings.Settings

// Ustawienia zapamiętane na tym urządzeniu, wspólne dla wszystkich kont.
// Android: SharedPreferences aplikacji, Windows: rejestr użytkownika (java.util.prefs).
object Ustawienia {
    // Na Windows klucze leżą we wspólnym miejscu wszystkich programów Javy, stąd przedrostek
    private const val ZDJECIA_W_WYNIKACH = "magazyn_ur.zdjecia_w_wynikach"

    private val ustawienia: Settings by lazy { Settings() }

    // Miniatury zdjęć w wynikach wyszukiwania (pobierają zdjęcia, więc domyślnie wyłączone)
    var zdjeciaWWynikach: Boolean
        get() = runCatching { ustawienia.getBoolean(ZDJECIA_W_WYNIKACH, false) }.getOrDefault(false)
        set(wartosc) {
            // Nieudany zapis tylko sprawi, że po ponownym uruchomieniu pole będzie odznaczone
            runCatching { ustawienia.putBoolean(ZDJECIA_W_WYNIKACH, wartosc) }
        }
}
