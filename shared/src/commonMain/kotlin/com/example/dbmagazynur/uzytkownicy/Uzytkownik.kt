package com.example.dbmagazynur.uzytkownicy

import com.example.dbmagazynur.sesja.emailNaLogin
import java.text.Collator
import java.util.Locale
import kotlinx.serialization.Serializable

// Wiersz tabeli uzytkownicy
@Serializable
data class Uzytkownik(val id: String, val nazwa: String, val poziom: Int)

val POZIOMY = 0..3

fun nazwaPoziomu(poziom: Int): String = when (poziom) {
    0 -> "Nieaktywne"
    1 -> "Pracownik"
    2 -> "Lider zmiany"
    3 -> "Administrator"
    else -> "Poziom $poziom"
}

fun opisPoziomu(poziom: Int): String = when (poziom) {
    0 -> "brak dostępu, historia zostaje"
    1 -> "wyszukiwanie, wydania, przyjęcia, materiały"
    2 -> "jak pracownik, plus księgowanie w SAP i anulowanie wydań"
    3 -> "jak lider, plus użytkownicy i maszyny"
    else -> ""
}

// Ta sama reguła co w Edge Function „uzytkownicy” (LOGIN w index.ts)
private val LOGIN = Regex("^[a-z0-9][a-z0-9._-]{1,49}$")

fun normalizujLogin(tekst: String): Result<String> {
    val login = tekst.trim().lowercase()
    return when {
        login.isEmpty() -> Result.failure(IllegalArgumentException("Wpisz login"))
        '@' in login -> Result.failure(IllegalArgumentException("Wpisz sam login, bez @ i domeny"))
        !LOGIN.matches(login) -> Result.failure(
            IllegalArgumentException(
                "Login: 2–50 znaków, małe litery bez polskich znaków, cyfry, kropka, myślnik lub podkreślenie",
            ),
        )
        else -> Result.success(login)
    }
}

// Aktywni najpierw, potem alfabetycznie po polsku
fun posortujUzytkownikow(lista: List<Uzytkownik>): List<Uzytkownik> {
    val polski = Collator.getInstance(Locale.forLanguageTag("pl-PL"))
    return lista.sortedWith(compareBy<Uzytkownik> { it.poziom == 0 }.thenBy(polski) { emailNaLogin(it.nazwa) })
}
