package com.example.aplicacionlesaa.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.R
import com.example.aplicacionlesaa.model.MuestraData

class FoliosAdapter(
    private val folios: List<MuestraData>,
    private val onClick: (MuestraData) -> Unit
) : RecyclerView.Adapter<FoliosAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFolio: TextView = view.findViewById(R.id.tvFolio)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folio, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = folios.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = folios[position]
        holder.tvFolio.text = data.folio
        holder.tvFecha.text = data.planMuestreo ?: "-"

        holder.itemView.setOnClickListener {
            onClick(data)
        }
    }
}
