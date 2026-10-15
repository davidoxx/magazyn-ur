package com.example.dbmagazynur.data

import kotlinx.serialization.json.Json

// Nowe kolumny w bazie nie mogą psuć starszej wersji aplikacji
val jsonBazy = Json { ignoreUnknownKeys = true }
