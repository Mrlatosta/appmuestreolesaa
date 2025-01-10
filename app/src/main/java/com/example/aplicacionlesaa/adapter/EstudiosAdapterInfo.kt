package com.example.aplicacionlesaa.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.R
import com.example.aplicacionlesaa.model.Estudios
import com.example.aplicacionlesaa.model.Servicio

class EstudiosAdapterInfo(private val estudiosList: List<Estudios>): RecyclerView.Adapter<EstudiosAdapterInfo.ServicioViewHolder>() {

    class ServicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIdServicio: TextView = itemView.findViewById(R.id.id)
        val tvClasificacion: TextView = itemView.findViewById(R.id.clasificacion)
        val tvclave_interna: TextView = itemView.findViewById(R.id.clave_interna)
        val tvnorma: TextView = itemView.findViewById(R.id.norma)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicioViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_estudios_info, parent, false)
        return ServicioViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ServicioViewHolder, position: Int) {
        val currentItem = estudiosList[position]
        holder.tvIdServicio.text = "ID Servicio: ${currentItem.id.toString()}"
        holder.tvClasificacion.text = "Clasificacion: ${currentItem.clasificacion}"
        holder.tvclave_interna.text = "Clave interna: ${currentItem.clave_interna}"
        holder.tvnorma.text = "Norma: ${currentItem.norma}"

    }


    override fun getItemCount() = estudiosList.size

}