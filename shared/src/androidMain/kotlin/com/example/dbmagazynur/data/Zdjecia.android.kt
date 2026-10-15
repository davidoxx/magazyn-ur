package com.example.dbmagazynur.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import coil3.PlatformContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import okio.Path
import okio.Path.Companion.toOkioPath

actual fun katalogPamieciZdjec(context: PlatformContext): Path = context.cacheDir.resolve("zdjecia").toOkioPath()

// Zmniejsza i kompresuje zdjęcie przed wysłaniem. ImageDecoder sam obraca
// zdjęcie zgodnie z EXIF i czyta też HEIC z galerii. Wolne, wołać poza wątkiem głównym.
fun przygotujZdjecie(resolver: ContentResolver, uri: Uri): ByteArray {
    val bitmapa = try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            val (szerokosc, wysokosc) = rozmiarDocelowy(info.size.width, info.size.height)
            decoder.setTargetSize(szerokosc, wysokosc)
            // compress() potrzebuje bitmapy w zwykłej pamięci, nie w pamięci grafiki
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } catch (e: IOException) {
        // Bez tego komunikat() pokazałby błąd odczytu pliku jako brak połączenia
        throw IllegalArgumentException("Nie udało się odczytać zdjęcia", e)
    }
    return ByteArrayOutputStream().use { wyjscie ->
        bitmapa.compress(Bitmap.CompressFormat.JPEG, JAKOSC_JPEG, wyjscie)
        bitmapa.recycle()
        wyjscie.toByteArray()
    }
}

// Jeden stały plik w cache: kolejne zdjęcie z aparatu nadpisuje poprzednie.
// FileProvider o tej nazwie jest zadeklarowany w manifeście modułu app.
fun plikNaZdjecieZAparatu(context: Context): Uri {
    val katalog = File(context.cacheDir, "aparat").apply { mkdirs() }
    return FileProvider.getUriForFile(context, "${context.packageName}.pliki", File(katalog, "zdjecie.jpg"))
}
