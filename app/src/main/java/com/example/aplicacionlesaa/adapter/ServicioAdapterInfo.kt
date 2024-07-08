package com.example.aplicacionlesaa.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.R
import com.example.aplicacionlesaa.model.Servicio

class ServicioAdapterInfo(private val servicioList: List<Servicio>) : RecyclerView.Adapter<ServicioAdapterInfo.ServicioViewHolder>() {

    class ServicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIdServicio: TextView = itemView.findViewById(R.id.id)
        val tvCantidad: TextView = itemView.findViewById(R.id.cantidad)
        val tvEstudiosMicro: TextView = itemView.findViewById(R.id.estudios_microbiologicos)
        val tvEstudiosFisico: TextView = itemView.findViewById(R.id.estudios_fisicoquimicos)
        val tvDescripcion: TextView = itemView.findViewById(R.id.descripcion)
        val tvCantidadToma: TextView = itemView.findViewById(R.id.cantidad_de_toma)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicioViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_servicio_info, parent, false)
        return ServicioViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ServicioViewHolder, position: Int) {
        val currentItem = servicioList[position]
        holder.tvIdServicio.text = "ID Servicio: ${currentItem.id.toString()}"
        holder.tvCantidad.text = "Cantidad: ${currentItem.cantidad}"
        holder.tvEstudiosMicro.text = "Estudios Microbiologicos: ${currentItem.estudios_microbiologicos}"
        holder.tvEstudiosFisico.text = "Estudios Fisicoquimicos: ${currentItem.estudios_fisicoquimicos}"
        holder.tvDescripcion.text = "Descripcion: ${currentItem.descripcion}"
        holder.tvCantidadToma.text = "Cantidad de Toma: ${currentItem.cantidad_de_toma}"
    }


    override fun getItemCount() = servicioList.size
}
