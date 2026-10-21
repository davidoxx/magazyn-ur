package com.example.dbmagazynur.katalog

import coil3.PlatformContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

// Katalog pobrany z Supabase: pliki aplikacji na Androidzie, folder użytkownika na Windows
expect fun plikPobranegoKatalogu(context: PlatformContext): File

// Najpierw zapis obok, potem podmiana: przerwany zapis nie zostawi połowy pliku
internal fun zapiszAtomowo(plik: File, bajty: ByteArray) {
    plik.absoluteFile.parentFile?.mkdirs()
    val tymczasowy = File(plik.absoluteFile.parentFile, plik.name + ".tmp")
    tymczasowy.writeBytes(bajty)
    Files.move(tymczasowy.toPath(), plik.toPath(), StandardCopyOption.REPLACE_EXISTING)
}
