package com.example.aplicacionlesaa

import android.os.Parcel
import android.os.Parcelable

data class Muestra(
    var numeroMuestra: String,
    val fechaMuestra: String,
    var registroMuestra: String,
    var nombreMuestra: String,
    var idLab: String,
    var cantidadAprox: String,
    var tempM: String,
    var lugarToma: String,
    var descripcionM: String,
    var emicro: String,
    var efisico: String,
    var observaciones: String,
    val servicioId: String,
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
        parcel.readString() ?: "",
        parcel.readString() ?: "",

    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(numeroMuestra)
        parcel.writeString(fechaMuestra)
        parcel.writeString(registroMuestra)
        parcel.writeString(nombreMuestra)
        parcel.writeString(idLab)
        parcel.writeString(cantidadAprox)
        parcel.writeString(tempM)
        parcel.writeString(lugarToma)
        parcel.writeString(descripcionM)
        parcel.writeString(emicro)
        parcel.writeString(efisico)
        parcel.writeString(observaciones)
        parcel.writeString(servicioId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Muestra> {
        override fun createFromParcel(parcel: Parcel): Muestra {
            return Muestra(parcel)
        }

        override fun newArray(size: Int): Array<Muestra?> {
            return arrayOfNulls(size)
        }
    }
}
