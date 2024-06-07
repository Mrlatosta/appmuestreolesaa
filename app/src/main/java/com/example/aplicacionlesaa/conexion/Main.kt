package com.example.aplicacionlesaa.conexion

import java.sql.DriverManager

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val ids = mutableListOf<String>()

        val connection = DriverManager.getConnection(
            "jdbc:postgresql://db-grupolesaa-rds.c1qss02m236z.us-east-1.rds.amazonaws.com:5432/dbgrupolesaa",
            "postgres",
            "Lara1234"
        )
        connection.createStatement().use { stmt ->
            // Solicita el folio al usuario (puedes usar Scanner o cualquier otro método)
            println("Ingresa el folio deseado:")
            val folioUsuario = "FCLHTL-LAB-062-COT-001"// Lee el folio ingresado por el usuario

            // Consulta SQL con el folio proporcionado por el usuario
            val query = """
            SELECT servicios.id, estudios.clasificacion, servicios.cantidad, servicios.descripcion,
                   servicios.estudios_microbiologicos, servicios.estudios_fisicoquimicos
            FROM servicios
            JOIN estudios ON estudios.clave_interna = servicios.estudio_clave_interna
            WHERE servicios.folio_id = ?
        """.trimIndent()

            // Prepara la consulta con el folio proporcionado por el usuario
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setString(1, folioUsuario) // Asigna el folio ingresado al parámetro

            // Ejecuta la consulta
            preparedStatement.executeQuery().use { rs ->
                while (rs.next()) {
                    val id = rs.getString("id")
                    println("ID: $id")
                    ids.add(id)
                    println(ids)
                }
            }

        }


    }
}
