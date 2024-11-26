package com.example.aplicacionlesaa.adapter

import android.text.Editable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.databinding.ItemFisicoquimicosBinding
import com.example.aplicacionlesaa.model.analisisFisico

class analisisFisicoViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val binding = ItemFisicoquimicosBinding.bind(view)


//    val numMuestra = view.findViewById<TextView>(R.id.tvnumMuestra)
//    val idMuestra = view.findViewById<TextView>(R.id.tvidMuestra)
//    val descripcionMuestra = view.findViewById<TextView>(R.id.tvDescripcion)
//    val fechaMuestreo = view.findViewById<TextView>(R.id.tvFechaMuestreo)


    fun render(analisisF: analisisFisico, onclickEdit: (Int) -> Unit) {

        binding.tvRegistroMuestra.text = analisisF.registro_muestra
        binding.tvnombreMuestra.text = analisisF.nombre_muestra
        binding.tvhoraAnalisis.text = analisisF.hora_analisis
        binding.tvtemp.text = analisisF.temperatura
        binding.tvPH.text = Editable.Factory.getInstance().newEditable(analisisF.ph)






        binding.btnEditarFQ.setOnClickListener{onclickEdit(adapterPosition)}



    }

}