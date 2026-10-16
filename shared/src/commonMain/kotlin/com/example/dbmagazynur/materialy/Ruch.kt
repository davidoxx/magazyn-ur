package com.example.dbmagazynur.materialy

import com.example.dbmagazynur.maszyny.Maszyna
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// UTWORZENIE: wpis przy dodaniu materiału, ilosc = stan początkowy (może być 0)
@Serializable
enum class TypRuchu { WYDANIE, PRZYJECIE, UTWORZENIE }

@Serializable
data class Osoba(val nazwa: String)

// Wiersz tabeli ruchy z dołączonymi osobami i maszyną (zapytanie KOLUMNY_HISTORII)
@Serializable
data class Ruch(
    val id: Long,
    val typ: TypRuchu,
    val ilosc: Double,
    val uwagi: String? = null,
    // Przy przyjęciu i utworzeniu materiału (opcjonalny)
    @SerialName("nr_zamowienia") val nrZamowienia: String? = null,
    val utworzono: String,
    @SerialName("zaksiegowano_sap") val zaksiegowanoSap: String? = null,
    val anulowano: String? = null,
    val kto: Osoba? = null,
    @SerialName("kto_ksiegowal") val ktoKsiegowal: Osoba? = null,
    @SerialName("kto_anulowal") val ktoAnulowal: Osoba? = null,
    val maszyna: Maszyna? = null,
)

// ruchy ma trzy klucze obce do uzytkownicy, więc każde dołączenie wskazuje swój klucz
const val KOLUMNY_HISTORII =
    "id,typ,ilosc,uwagi,nr_zamowienia,utworzono,zaksiegowano_sap,anulowano," +
        "kto:uzytkownicy!ruchy_uzytkownik_fkey(nazwa)," +
        "kto_ksiegowal:uzytkownicy!ruchy_zaksiegowal_fkey(nazwa)," +
        "kto_anulowal:uzytkownicy!ruchy_anulowal_fkey(nazwa)," +
        "maszyna:maszyny(id,nazwa,kod)"

enum class StatusRuchu { ANULOWANY, ZAKSIEGOWANY, CZEKA_NA_SAP }

// Status ważny dla pracownika i lidera. Księgowanie w SAP dotyczy tylko wydań materiałów SAP.
fun statusRuchu(typ: TypRuchu, anulowano: String?, zaksiegowanoSap: String?, materialSap: Boolean): StatusRuchu? =
    when {
        anulowano != null -> StatusRuchu.ANULOWANY
        typ != TypRuchu.WYDANIE -> null
        zaksiegowanoSap != null -> StatusRuchu.ZAKSIEGOWANY
        materialSap -> StatusRuchu.CZEKA_NA_SAP
        else -> null
    }

fun Ruch.status(materialSap: Boolean): StatusRuchu? = statusRuchu(typ, anulowano, zaksiegowanoSap, materialSap)
