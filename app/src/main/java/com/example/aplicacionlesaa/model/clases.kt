package com.example.aplicacionlesaa.model

import android.os.Parcel
import android.os.Parcelable
import com.example.aplicacionlesaa.Muestra


data class Plandemuestreo(

    val nombre_pdm: String,

)

data class Servicio(
    val id: String,
    var cantidad: Int,
    var estudios_microbiologicos: String,
    val estudios_fisicoquimicos: String,
    val descripcion: String,
    val cantidad_de_toma: String,
    val clasificacion: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeInt(cantidad)
        parcel.writeString(estudios_microbiologicos)
        parcel.writeString(estudios_fisicoquimicos)
        parcel.writeString(descripcion)
        parcel.writeString(cantidad_de_toma)
        parcel.writeString(clasificacion)
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
) : Parcelable{
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(descripcion)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Descripcion> {
        override fun createFromParcel(parcel: Parcel): Descripcion {
            return Descripcion(parcel)
        }

        override fun newArray(size: Int): Array<Descripcion?> {
            return arrayOfNulls(size)
        }
    }
}

data class Pdm(
    val nombre_pdm: String,
    val pq_atendera: String,
    val folio_id_cot: String,
    val fecha_hora_cita: String,
    val ingeniero_campo: String,
    val nombre_lugar: String,
    val nombre_empresa: String
) : Parcelable {
    constructor(parcel: Parcel): this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""


    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(nombre_pdm)
        parcel.writeString(pq_atendera)
        parcel.writeString(folio_id_cot)
        parcel.writeString(fecha_hora_cita)
        parcel.writeString(ingeniero_campo)
        parcel.writeString(nombre_lugar)
        parcel.writeString(nombre_empresa)

    }
    override fun describeContents(): Int {
        return 0
    }
    companion object CREATOR : Parcelable.Creator<Pdm> {
        override fun createFromParcel(parcel: Parcel): Pdm {
            return Pdm(parcel)
        }
        override fun newArray(size: Int): Array<Pdm?> {
            return arrayOfNulls(size)
        }
    }
}

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

data class UltimoFolio(
    val folio: String
)

data class Muestra_pdm(
    val registro_muestra: String,
    val folio_muestreo: String,
    val fecha_muestreo: String,
    val nombre_muestra: String,
    val id_lab: String,
    val cantidad_aprox: String,
    val temperatura: String,
    val lugar_toma: String,
    val descripcion_toma: String,
    val e_micro: String,
    val e_fisico: String,
    val observaciones: String,
    val folio_pdm: String,
    val servicio_id: String,
    val estatus: String = "Pendiente",
    val subtipo: String,
)

data class Muestra_pdmExtra(
    val registro_muestra: String,
    val folio_muestreo: String,
    val fecha_muestreo: String,
    val nombre_muestra: String,
    val id_lab: String,
    val cantidad_aprox: String,
    val temperatura: String,
    val lugar_toma: String,
    val descripcion_toma: String,
    val e_micro: String,
    val e_fisico: String,
    val observaciones: String,
    val folio_pdm: String,
    val estudio_id: Int,
    val estatus: String = "Pendiente",
)

data class MuestraData(
    val folio: String,
    val planMuestreo: String,
    val clientePdm: ClientePdm?,
    val serviciosPdm: List<Servicio>,
    val muestras: List<Muestra>
)

data class DatosFinalesFolioMuestreo(
    val nombre_autoriza_muestras: String,
    val puesto_autoriza_muestra: String,
    val nombre_tomador_muestra: String,
    val puesto_tomador_muestra: String,
)

data class Lugares(
    val nombre_lugar: String,
)

data class Lugar(
    val cliente_folio: String,
    val nombre_lugar: String,
    val folio_pdm: String
)

data class analisisFisico(
    val registro_muestra: String,
    val nombre_muestra: String,
    val hora_analisis: String,
    val temperatura: String,
    var ph: String,
    var clt: Int?,
    var clr: Int?,
    var crnas: Int?,
    var cya: Int?,
    var tur: Int?,
)





