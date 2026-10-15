package com.example.dbmagazynur.sesja

// Konta pracowników zakładamy w panelu Supabase na techniczne adresy w tej domenie
private const val DOMENA = "@magazyn.local"

// Pracownik wpisuje sam login. Pełny e-mail (konto administratora) zostaje bez domeny.
fun loginNaEmail(login: String): String {
    val znormalizowany = login.trim().lowercase()
    return if ('@' in znormalizowany) znormalizowany else znormalizowany + DOMENA
}

fun emailNaLogin(email: String): String = email.removeSuffix(DOMENA)
