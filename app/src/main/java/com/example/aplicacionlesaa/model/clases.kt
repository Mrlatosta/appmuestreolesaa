package com.example.aplicacionlesaa.model

import android.os.Parcel
import android.os.Parcelable


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
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(cantidad)
        parcel.writeString(estudios_microbiologicos)
        parcel.writeString(estudios_fisicoquimicos)
        parcel.writeString(descripcion)
        parcel.writeString(cantidad_de_toma)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Servicio> {
        override fun createFromParcel(parcel: Parcel): Servicio {
            return Servicio(parcel)
        }

        override fun newArray(size: Int): Array<Servicio?> {
            return arrayOfNulls(size)
        }
    }
}

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
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(giro)
        parcel.writeString(nombre_empresa)
        parcel.writeString(folio)
        parcel.writeString(direccion)
        parcel.writeString(estado)
        parcel.writeString(atencion)
        parcel.writeString(departamento)
        parcel.writeString(puesto)
        parcel.writeString(giro_empresa)
        parcel.writeString(telefono)
        parcel.writeString(correo)
        parcel.writeString(rfc)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ClientePdm> {
        override fun createFromParcel(parcel: Parcel): ClientePdm {
            return ClientePdm(parcel)
        }

        override fun newArray(size: Int): Array<ClientePdm?> {
            return arrayOfNulls(size)
        }
    }
}

data class FolioMuestreo(
    val folio: String,
    val fecha: String,
    val folio_cliente: String,
    val folio_pdm: String
)




