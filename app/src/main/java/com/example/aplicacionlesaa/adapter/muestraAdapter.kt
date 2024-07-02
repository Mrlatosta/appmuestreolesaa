package com.example.aplicacionlesaa.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.Muestra
import com.example.aplicacionlesaa.R
import com.example.aplicacionlesaa.utils.OnItemMovedListener
import java.util.Collections

class muestraAdapter(
    private val muestraList: MutableList<Muestra>,
    private val onClickListener: (Muestra) -> Unit,
    private val onclickDelete: (Int) -> Unit,
    private val onclickEdit: (Int) -> Unit,
    private val onItemMovedListener: OnItemMovedListener
) : RecyclerView.Adapter<MuestraViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MuestraViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return MuestraViewHolder(layoutInflater.inflate(R.layout.item_muestra, parent, false))
    }

    override fun getItemCount(): Int {
        return muestraList.size
    }

    override fun onBindViewHolder(holder: MuestraViewHolder, position: Int) {
        val item = muestraList[position]
        holder.render(item, onClickListener, onclickDelete, onclickEdit)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {

        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(muestraList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(muestraList, i, i - 1)
            }
        }
        val muestraFrom = muestraList[fromPosition]
        val muestraTo = muestraList[toPosition]

        // Intercambiar valores específicos
        val tempNumeroMuestra = muestraFrom.numeroMuestra
        val tempRegistroMuestra = muestraFrom.registroMuestra
        val tempIdLab = muestraFrom.idLab

        muestraFrom.numeroMuestra = muestraTo.numeroMuestra
        muestraFrom.registroMuestra = muestraTo.registroMuestra
        muestraFrom.idLab = muestraTo.idLab

        muestraTo.numeroMuestra = tempNumeroMuestra
        muestraTo.registroMuestra = tempRegistroMuestra
        muestraTo.idLab = tempIdLab

        // Notificar que se movió un item

        notifyItemMoved(fromPosition, toPosition)
        notifyItemChanged(fromPosition)
        notifyItemChanged(toPosition)
        onItemMovedListener.onItemMoved()


    }


}


