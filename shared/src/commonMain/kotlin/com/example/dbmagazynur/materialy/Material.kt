package com.example.dbmagazynur.materialy

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TypMaterialu { SAP, KOSZTOWY }

// Wiersz tabeli materialy (pola jak kolumny w bazie)
@Serializable
data class Material(
    val id: Long,
    val nazwa: String,
    val opis: String? = null,
    val typ: TypMaterialu,
    @SerialName("nr_sap") val nrSap: String? = null,
    val miejsce: String? = null,
    val ilosc: Double,
    val jednostka: String,
    val zdjecie: String? = null,
)

// Ilość po polsku: "12", "12,5", "0,125" (baza trzyma do 3 miejsc po przecinku)
fun formatujIlosc(ilosc: Double): String =
    DecimalFormat("0.###", DecimalFormatSymbols(Locale.forLanguageTag("pl-PL"))).format(ilosc)
