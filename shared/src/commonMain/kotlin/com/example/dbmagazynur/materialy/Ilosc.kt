package com.example.dbmagazynur.materialy

import java.math.BigDecimal

// Największa ilość, jaką zmieści numeric(12,3) w bazie
private val MAKS_ILOSC = BigDecimal("999999999.999")

// Ilość wpisana przez pracownika: "2,5" albo "2.5". Baza trzyma 3 miejsca
// po przecinku i więcej by po cichu zaokrągliła, więc odrzucamy to tutaj.
// Zero ma sens tylko jako stan początkowy nowego materiału.
fun parsujIlosc(tekst: String, dopuscZero: Boolean = false): Result<BigDecimal> {
    val znormalizowany = tekst.trim().replace(',', '.')
    if (znormalizowany.isEmpty()) return blad("Wpisz ilość")
    val ilosc = znormalizowany.toBigDecimalOrNull() ?: return blad("Nieprawidłowa ilość")
    return when {
        ilosc.signum() < 0 -> blad("Ilość nie może być ujemna")
        ilosc.signum() == 0 && !dopuscZero -> blad("Ilość musi być większa od zera")
        ilosc.stripTrailingZeros().scale() > 3 -> blad("Najwyżej 3 miejsca po przecinku")
        ilosc > MAKS_ILOSC -> blad("Za duża ilość")
        else -> Result.success(ilosc)
    }
}

private fun blad(komunikat: String): Result<BigDecimal> =
    Result.failure(IllegalArgumentException(komunikat))
