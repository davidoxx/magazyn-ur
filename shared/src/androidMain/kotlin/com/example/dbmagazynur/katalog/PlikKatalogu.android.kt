package com.example.dbmagazynur.katalog

import coil3.PlatformContext
import java.io.File

// Pliki aplikacji, nie pamięć podręczna: system nie usunie katalogu, gdy brakuje miejsca
actual fun plikPobranegoKatalogu(context: PlatformContext): File = File(context.filesDir, "katalog.json")
