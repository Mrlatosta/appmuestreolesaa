package com.example.aplicacionlesaa.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.R
import com.example.aplicacionlesaa.model.Servicio

class servicioAdapter(
    private val servicioList: List<Servicio>,
    private val onClickListener: (Servicio) -> Unit,
    private val onclickDelete:(Int) -> Unit
) : RecyclerView.Adapter<ServicioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicioViewHolder {
        //Elview holder es el encargado con el objeto que contiene de agarrar los atributos y pintarlos
        //Revuelve ese item al view holder, osea el xml a aca
        val layoutInflater = LayoutInflater.from(parent.context)
        return ServicioViewHolder(layoutInflater.inflate(R.layout.item_servicio, parent, false))
    }

    override fun getItemCount(): Int {
        return servicioList.size
    }

    override fun onBindViewHolder(holder: ServicioViewHolder, position: Int) {

        //Pasa por cada uno de los items y va a llamar al fun render, devuelve la isntancia del viewholder y la posicion
        val item = servicioList[position]
        holder.render(item, onClickListener,onclickDelete)

    }


}