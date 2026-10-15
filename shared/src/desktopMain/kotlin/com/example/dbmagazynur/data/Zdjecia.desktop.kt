package com.example.dbmagazynur.data

import coil3.PlatformContext
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import okio.Path
import okio.Path.Companion.toOkioPath

// %LOCALAPPDATA%\MagazynUR\zdjecia, a gdyby go nie było, folder użytkownika
actual fun katalogPamieciZdjec(context: PlatformContext): Path {
    val baza = System.getenv("LOCALAPPDATA")?.let(::File) ?: File(System.getProperty("user.home"), ".magazynur")
    return File(baza, "MagazynUR/zdjecia").toOkioPath()
}

// Zmniejsza i kompresuje zdjęcie z dysku tak samo jak na telefonie (1280 px, JPEG 80%).
// Obsługuje JPG, PNG, BMP i GIF. Wolne, wołać poza wątkiem głównym.
fun przygotujZdjecie(plik: File): ByteArray {
    val obraz = try {
        ImageIO.read(plik)
    } catch (e: IOException) {
        null
    } ?: throw IllegalArgumentException("Nie udało się odczytać zdjęcia. Wybierz plik JPG albo PNG.")

    val (szerokosc, wysokosc) = rozmiarDocelowy(obraz.width, obraz.height)
    val zmniejszony = BufferedImage(szerokosc, wysokosc, BufferedImage.TYPE_INT_RGB)
    zmniejszony.createGraphics().apply {
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        // JPEG nie ma przezroczystości: przezroczyste tło z PNG robi się białe
        color = Color.WHITE
        fillRect(0, 0, szerokosc, wysokosc)
        drawImage(obraz, 0, 0, szerokosc, wysokosc, null)
        dispose()
    }

    val pisarz = ImageIO.getImageWritersByFormatName("jpg").next()
    val parametry = pisarz.defaultWriteParam.apply {
        compressionMode = ImageWriteParam.MODE_EXPLICIT
        compressionQuality = JAKOSC_JPEG / 100f
    }
    return ByteArrayOutputStream().use { wyjscie ->
        ImageIO.createImageOutputStream(wyjscie).use { strumien ->
            pisarz.output = strumien
            pisarz.write(null, IIOImage(zmniejszony, null, null), parametry)
        }
        pisarz.dispose()
        wyjscie.toByteArray()
    }
}
