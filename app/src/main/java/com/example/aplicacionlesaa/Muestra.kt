package com.example.aplicacionlesaa

import android.os.Parcel
import android.os.Parcelable

data class Muestra(
    var numeroMuestra: String,
    val fechaMuestra: String,
    val horaMuestra: String,
    var registroMuestra: String,
    val nombreMuestra: String,
    val idLab: String,
    val cantidadAprox: String,
    val tempM: String,
    val lugarToma: String,
    val descripcionM: String,
    val emicro: String,
    val efisico: String,
    val observaciones: String
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
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(numeroMuestra)
        parcel.writeString(fechaMuestra)
        parcel.writeString(horaMuestra)
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
