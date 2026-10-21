package com.example.dbmagazynur.katalog

import coil3.PlatformContext
import java.io.File

// %LOCALAPPDATA%\MagazynUR\katalog.json (obok pamięci zdjęć), a gdyby go nie było, folder użytkownika
actual fun plikPobranegoKatalogu(context: PlatformContext): File {
    val baza = System.getenv("LOCALAPPDATA")?.let(::File) ?: File(System.getProperty("user.home"), ".magazynur")
    return File(baza, "MagazynUR/katalog.json")
}
