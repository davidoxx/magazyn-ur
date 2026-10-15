package com.example.dbmagazynur.data

import java.text.Normalizer

private val ZNAKI_DIAKRYTYCZNE = Regex("\\p{Mn}+")
private val ODSTEPY = Regex("\\s+")

// Tekst bez wielkości liter i polskich znaków, tak jak szukaj_materialow w bazie.
// "ł" nie rozkłada się na literę i znak, więc zamieniamy je osobno.
fun doPorownania(tekst: String): String =
    Normalizer.normalize(tekst.lowercase(), Normalizer.Form.NFD)
        .replace(ZNAKI_DIAKRYTYCZNE, "")
        .replace('ł', 'l')

// Każde słowo frazy musi wystąpić w tekście (szukanie po stronie telefonu)
fun pasujeDoFrazy(tekst: String, fraza: String): Boolean {
    val gdzie = doPorownania(tekst)
    return doPorownania(fraza).split(ODSTEPY).filter { it.isNotEmpty() }.all { it in gdzie }
}
