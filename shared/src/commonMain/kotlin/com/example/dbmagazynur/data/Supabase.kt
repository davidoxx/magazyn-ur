package com.example.dbmagazynur.data

import com.example.dbmagazynur.Konfiguracja
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.coil.Coil3Integration
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage

// Jeden klient Supabase na całą aplikację. Sesję logowania zapisuje i odświeża Auth
// (na Androidzie w ustawieniach aplikacji, na Windows w rejestrze użytkownika).
val supabase = createSupabaseClient(
    supabaseUrl = Konfiguracja.SUPABASE_URL,
    supabaseKey = Konfiguracja.SUPABASE_KEY,
) {
    defaultSerializer = KotlinXSerializer(jsonBazy)
    install(Auth)
    install(Postgrest)
    install(Storage)
    install(Coil3Integration)
    install(Functions)
}
