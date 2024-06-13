package com.example.aplicacionlesaa.model


data class Plandemuestreo(

    val nombre_pdm: String,

)

data class Servicio(
    val id: Int,
    var cantidad: Int,
    val estudios_microbiologicos: String,
    val estudios_fisicoquimicos: String,
    val descripcion: String,
    val cantidad_de_toma: String
)

data class Descripcion(
    val id: Int,
    val descripcion: String
)

data class Pdm(
    val nombre_pdm: String,
    val pq_atendera: String,
    val folio_id_cot: String,
    val fecha_hora_cita: String,
    val ingeniero_campo: String
)

data class ClientePdm(

    val giro: String,
    val nombre_empresa: String,
    val folio: String,
    val direccion: String,
    val estado: String,
    val atencion: String,
    val departamento: String,
    val puesto: String,
    val giro_empresa: String,
    val telefono: String,
    val correo: String,
    val rfc: String
)


