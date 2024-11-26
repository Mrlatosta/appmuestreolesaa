package com.example.aplicacionlesaa.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.R
import com.example.aplicacionlesaa.model.analisisFisico

class analisisFisicoAdapter(
    private val lista: MutableList<analisisFisico>,
    private val onEdit: (Int) -> Unit
) : RecyclerView.Adapter<analisisFisicoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): analisisFisicoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fisicoquimicos, parent, false)
        return analisisFisicoViewHolder(view)
    }

    override fun onBindViewHolder(holder: analisisFisicoViewHolder, position: Int) {
        holder.bind(lista[position], onEdit)
    }

    override fun getItemCount(): Int = lista.size

    // Método para obtener la lista actualizada
    fun obtenerDatosActualizados(): List<analisisFisico> {
        return lista
    }
}

