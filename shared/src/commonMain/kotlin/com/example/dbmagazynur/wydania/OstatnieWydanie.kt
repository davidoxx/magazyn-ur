package com.example.dbmagazynur.wydania

import com.example.dbmagazynur.materialy.Material
import com.example.dbmagazynur.materialy.StatusRuchu
import com.example.dbmagazynur.materialy.TypMaterialu
import com.example.dbmagazynur.materialy.TypRuchu
import com.example.dbmagazynur.materialy.statusRuchu
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wiersz widoku ostatnie_wydania. material to cały wiersz materiału (to_jsonb w widoku),
// więc kliknięcie od razu otwiera jego kartę.
@Serializable
data class OstatnieWydanie(
    val id: Long,
    val utworzono: String,
    val ilosc: Double,
    val uwagi: String? = null,
    @SerialName("zaksiegowano_sap") val zaksiegowanoSap: String? = null,
    val anulowano: String? = null,
    val material: Material,
    val wydal: String,
    @SerialName("maszyna_kod") val maszynaKod: String? = null,
    val maszyna: String? = null,
) {
    val status: StatusRuchu?
        get() = statusRuchu(TypRuchu.WYDANIE, anulowano, zaksiegowanoSap, material.typ == TypMaterialu.SAP)
}

// Wartość = typ_materialu w widoku (null = bez filtra)
enum class FiltrWydan(val etykieta: String, val typMaterialu: String?) {
    WSZYSTKIE("Wszystkie", null),
    KOSZTOWE("Kosztowe", "KOSZTOWY"),
    SAP("SAP", "SAP"),
}

data class StanOstatnichWydan(
    val filtr: FiltrWydan = FiltrWydan.WSZYSTKIE,
    // null = jeszcze nie wczytane
    val wydania: List<OstatnieWydanie>? = null,
    val saStarsze: Boolean = false,
    val wczytywanie: Boolean = false,
    val blad: String? = null,
)
