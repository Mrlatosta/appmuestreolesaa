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


    fun render(
        muestra: Muestra,
        onClickListener: (Muestra) -> Unit,
        onclickDelete: (Int) -> Unit,
        onclickEdit: (Int) -> Unit
    ) {

        binding.tvnumMuestra.text = muestra.numeroMuestra.toString()
        binding.tvfecham.text = muestra.fechaMuestra.toString()
        binding.tvregistroM.text = muestra.registroMuestra
        binding.tvnombre.text = muestra.nombreMuestra
        binding.tvcantidadAprox.text = muestra.cantidadAprox
        binding.tvtemperatura.text = muestra.tempM
        binding.tvlugar.text = muestra.lugarToma
        binding.tvdescripcion.text = muestra.descripcionM
        binding.tvMicro.text = muestra.emicro
        binding.tvFisico.text = muestra.efisico
        binding.tvObservaciones.text = muestra.observaciones
        binding.tvServicioIDMuestras.text = "ID: " + muestra.servicioId

//        itemView.setOnClickListener { onClickListener(muestra) }

        binding.btnDelete.setOnClickListener{onclickDelete(adapterPosition)}
        binding.btnEditar.setOnClickListener{onclickEdit(adapterPosition)}
        binding.btnCopiar.setOnClickListener{onClickListener(muestra)}

    }

}