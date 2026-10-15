package com.example.dbmagazynur.data

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.key.Keyer
import coil3.request.Options
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.coil.coil3
import io.github.jan.supabase.storage.StorageItem
import java.util.UUID
import kotlin.math.roundToInt
import okio.Path

// Prywatny bucket Storage na zdjęcia materiałów
const val BUCKET_ZDJEC = "zdjecia"

// Darmowy plan ma 1 GB na pliki: dłuższy bok 1280 px i JPEG 80% to zwykle 150–250 kB
internal const val MAKS_BOK_ZDJECIA = 1280
internal const val JAKOSC_JPEG = 80

// Każde zdjęcie dostaje nową nazwę. Pamięć podręczna Coila jest kluczowana ścieżką,
// więc przy stałej nazwie telefony pokazywałyby stare zdjęcie po podmianie.
fun nowaSciezkaZdjecia(materialId: Long): String = "materialy/$materialId/${UUID.randomUUID()}.jpg"

fun rozmiarDocelowy(szerokosc: Int, wysokosc: Int, maksBok: Int = MAKS_BOK_ZDJECIA): Pair<Int, Int> {
    val dluzszy = maxOf(szerokosc, wysokosc)
    if (dluzszy <= maksBok) return szerokosc to wysokosc
    val skala = maksBok.toDouble() / dluzszy
    return (szerokosc * skala).roundToInt().coerceAtLeast(1) to
        (wysokosc * skala).roundToInt().coerceAtLeast(1)
}

// Gdzie trzymać pobrane zdjęcia: cache aplikacji na Androidzie, folder użytkownika na Windows
expect fun katalogPamieciZdjec(context: PlatformContext): Path

// Coil pobiera zdjęcia ze Storage jako zalogowany użytkownik (bucket jest prywatny).
// Integracja supabase-kt z Coilem jest oznaczona jako eksperymentalna.
@OptIn(SupabaseExperimental::class)
fun zbudujImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .components {
            add(supabase.coil3)
            // Bez klucza Coil nie trzymałby zdjęć ze Storage w pamięci
            add(
                object : Keyer<StorageItem> {
                    override fun key(data: StorageItem, options: Options) = "${data.bucketId}/${data.path}"
                },
            )
        }
        .diskCache {
            DiskCache.Builder()
                .directory(katalogPamieciZdjec(context))
                .maxSizeBytes(100L * 1024 * 1024)
                .build()
        }
        .build()
