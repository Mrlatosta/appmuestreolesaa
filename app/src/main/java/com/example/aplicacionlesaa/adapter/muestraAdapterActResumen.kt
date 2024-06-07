package com.example.aplicacionlesaa.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.Muestra
import com.example.aplicacionlesaa.R

class muestraAdapterActResumen(
    private val muestraList: List<Muestra>,
    private val onClickListener: (Muestra) -> Unit,
    private val onclickDelete:(Int) -> Unit
) : RecyclerView.Adapter<MuestraViewHolderActResumen>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MuestraViewHolderActResumen {
        //Elview holder es el encargado con el objeto que contiene de agarrar los atributos y pintarlos
        //Revuelve ese item al view holder, osea el xml a aca
        val layoutInflater = LayoutInflater.from(parent.context)
        return MuestraViewHolderActResumen(layoutInflater.inflate(R.layout.item_muestra, parent, false))
    }

    override fun getItemCount(): Int {
        return muestraList.size
    }

    override fun onBindViewHolder(holder: MuestraViewHolderActResumen, position: Int) {

        //Pasa por cada uno de los items y va a llamar al fun render, devuelve la isntancia del viewholder y la posicion
        val item = muestraList[position]
        holder.render(item, onClickListener,onclickDelete)

    }


}