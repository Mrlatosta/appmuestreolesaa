package com.example.aplicacionlesaa.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.Muestra
import com.example.aplicacionlesaa.databinding.ItemMuestraBinding

class MuestraViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val binding = ItemMuestraBinding.bind(view)


//    val numMuestra = view.findViewById<TextView>(R.id.tvnumMuestra)
//    val idMuestra = view.findViewById<TextView>(R.id.tvidMuestra)
//    val descripcionMuestra = view.findViewById<TextView>(R.id.tvDescripcion)
//    val fechaMuestreo = view.findViewById<TextView>(R.id.tvFechaMuestreo)


    fun render(muestra: Muestra, onClickListener: (Muestra) -> Unit, onclickDelete: (Int) -> Unit) {

        binding.tvnumMuestra.text = muestra.numeroMuestra.toString()
        binding.tvfecham.text = muestra.fechaMuestra.toString()
        binding.tvhoram.text = muestra.horaMuestra
        binding.tvregistroM.text = muestra.registroMuestra
        binding.tvnombre.text = muestra.nombreMuestra

        itemView.setOnClickListener { onClickListener(muestra) }

        binding.btnDelete.setOnClickListener{onclickDelete(adapterPosition)}


    }

}