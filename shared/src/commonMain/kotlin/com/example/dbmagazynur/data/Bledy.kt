package com.example.dbmagazynur.data

import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import java.io.IOException
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Komunikat błędu do pokazania użytkownikowi. Funkcje w bazie zgłaszają błędy
// po polsku (raise exception), więc ich treść pokazujemy bez zmian.
fun Throwable.komunikat(): String = when (this) {
    is AuthWeakPasswordException -> when {
        "pwned" in reasons -> "To hasło wyciekło w internecie. Wybierz inne."
        "length" in reasons -> "Hasło jest za krótkie."
        else -> "Hasło jest za słabe. Użyj liter różnej wielkości i cyfr."
    }
    is AuthRestException -> when (errorCode) {
        AuthErrorCode.InvalidCredentials -> "Nieprawidłowy login lub hasło"
        AuthErrorCode.SamePassword -> "Nowe hasło musi być inne niż obecne."
        // Przy włączonej w Supabase opcji „Secure password change”
        // hasło można zmienić tylko krótko po zalogowaniu
        AuthErrorCode.ReauthenticationNeeded ->
            "Wyloguj się, zaloguj ponownie i od razu zmień hasło."
        else -> errorDescription
    }
    // Ograniczenia tabel zgłasza sam PostgreSQL, po angielsku (kody SQLSTATE)
    is PostgrestRestException -> when {
        code == "23505" && "nr_sap" in error -> "Materiał z tym numerem SAP już istnieje"
        code == "23505" && "maszyny_kod" in error -> "Maszyna z tym kodem już istnieje"
        code == "23505" -> "Taki wpis już istnieje"
        // Usuwanie wiersza, do którego odwołuje się historia ruchów
        code == "23503" && "\"maszyny\"" in error ->
            "Tej maszyny nie można usunąć, bo ma wydania w historii. Wycofaj ją zamiast tego."
        code == "23503" -> "Nie można usunąć, bo jest używane w historii ruchów."
        code == "42501" -> "Brak uprawnień"
        else -> error
    }
    is RestException -> bladZFunkcji(error) ?: error
    is IOException -> "Brak połączenia z serwerem. Sprawdź internet."
    else -> message ?: "Nieznany błąd"
}

// Edge Functions odpowiadają {"blad": "..."}, a supabase-kt podaje całą treść odpowiedzi
fun bladZFunkcji(tresc: String): String? = runCatching {
    jsonBazy.parseToJsonElement(tresc).jsonObject["blad"]?.jsonPrimitive?.content
}.getOrNull()
