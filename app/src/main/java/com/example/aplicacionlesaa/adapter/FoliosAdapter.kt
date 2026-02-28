package com.example.aplicacionlesaa.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.R
import com.example.aplicacionlesaa.model.MuestraData

class FoliosAdapter(
    initialFolios: List<MuestraData>,
    private val onClick: (MuestraData) -> Unit
) : RecyclerView.Adapter<FoliosAdapter.ViewHolder>() {

    private var folios: MutableList<MuestraData> = initialFolios.toMutableList()
    val selectedItems = mutableSetOf<MuestraData>()

    fun getSelectedItems(): List<MuestraData> {
        return selectedItems.toList()
    }

    fun updateData(newFolios: List<MuestraData>) {
        folios.clear()
        folios.addAll(newFolios)
        selectedItems.clear()
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFolio: TextView = view.findViewById(R.id.tvFolio)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val cbFolio: CheckBox = view.findViewById(R.id.cbFolio)
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
        holder.cbFolio.isChecked = selectedItems.contains(data)

        val clickListener = View.OnClickListener {
            if (selectedItems.contains(data)) {
                selectedItems.remove(data)
                holder.cbFolio.isChecked = false
            } else {
                selectedItems.add(data)
                holder.cbFolio.isChecked = true
            }
            onClick(data) // Notifica a la actividad para que actualice la vista de muestras
        }

        holder.itemView.setOnClickListener(clickListener)
        holder.cbFolio.setOnClickListener(clickListener)
    }
}
