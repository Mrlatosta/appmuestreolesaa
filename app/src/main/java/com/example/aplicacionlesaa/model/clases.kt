package com.example.aplicacionlesaa.model


data class Plandemuestreo(
    val id: Int,
    val nombre_pdm: String,
    val pq_atendera: String,
    val folioIdCot: String,
    val fecha_horacita: String,
    val ingeniero_campo: String

)

data class Servicio(
    val id: Int,
    var cantidad: Int,
    val estudios_microbiologicos: String,
    val estudios_fisicoquimicos: String,
    val descripcion: String,
    val cantidad_de_toma: String
)


