package com.example.aplicacionlesaa.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.R
import com.example.aplicacionlesaa.model.analisisFisico

class analisisFisicoAdapter(
    private val analisisfisico: List<analisisFisico>,
    private val onclickEdit: (Int) -> Unit,

) : RecyclerView.Adapter<analisisFisicoViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): analisisFisicoViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return analisisFisicoViewHolder(layoutInflater.inflate(R.layout.item_fisicoquimicos, parent, false))
    }

    override fun getItemCount(): Int {
        return analisisfisico.size
    }

    override fun onBindViewHolder(holder: analisisFisicoViewHolder, position: Int) {

        //Pasa por cada uno de los items y va a llamar al fun render, devuelve la isntancia del viewholder y la posicion
        val item = analisisfisico[position]
        holder.render(item,onclickEdit)

    }

}