package com.example.aplicacionlesaa.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.Muestra
import com.example.aplicacionlesaa.R

class MuestraResumenAdapter(
    private var muestras: List<Muestra>
) : RecyclerView.Adapter<MuestraResumenAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvLugar: TextView = view.findViewById(R.id.tvLugar)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_muestra_resumen, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val muestra = muestras[position]
        holder.tvNombre.text = muestra.nombreMuestra
        holder.tvLugar.text = muestra.lugarToma ?: "-"
        holder.tvFecha.text = muestra.fechaMuestra ?: "-"
    }

    override fun getItemCount(): Int = muestras.size

    fun updateData(nuevaLista: List<Muestra>) {
        muestras = nuevaLista
        notifyDataSetChanged()
    }
}
