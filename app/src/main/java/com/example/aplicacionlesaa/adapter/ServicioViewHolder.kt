package com.example.aplicacionlesaa.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView

import com.example.aplicacionlesaa.databinding.ItemServicioBinding
import com.example.aplicacionlesaa.model.Servicio

class ServicioViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val binding = ItemServicioBinding.bind(view)



    fun render(servicio: Servicio, onClickListener: (Servicio) -> Unit, onclickDelete: (Int) -> Unit) {

        binding.tvIdServicio.text = servicio.id.toString()
        binding.tvCantidad.text = servicio.cantidad.toString()
        binding.tvEstudiosMicro.text = servicio.estudios_microbiologicos
        binding.tvEstudiosFisico.text = servicio.estudios_fisicoquimicos
        binding.tvdescripcion.text = servicio.descripcion
        binding.tvCantidadToma.text = servicio.cantidad_de_toma





        itemView.setOnClickListener { onClickListener(servicio) }



    }

}