package com.example.dbmagazynur.sap

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wiersz widoku wydania_do_sap
@Serializable
data class WydanieDoSap(
    val id: Long,
    val utworzono: String,
    @SerialName("nr_sap") val nrSap: String,
    val material: String,
    val ilosc: Double,
    val jednostka: String,
    val wydal: String,
    val uwagi: String? = null,
    @SerialName("maszyna_kod") val maszynaKod: String? = null,
    val maszyna: String? = null,
)
