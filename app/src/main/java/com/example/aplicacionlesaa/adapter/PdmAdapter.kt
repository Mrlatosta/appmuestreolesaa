package com.example.aplicacionlesaa.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.R
import com.example.aplicacionlesaa.model.Pdm

class PdmAdapter(private val pdmList: List<Pdm>,private val onClickListener: (Pdm) -> Unit) : RecyclerView.Adapter<PdmAdapter.PdmViewHolder>() {

    class PdmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombrePdm: TextView = itemView.findViewById(R.id.nombre_pdm)
        val pqAtendera: TextView = itemView.findViewById(R.id.pq_atendera)
        val folioIdCot: TextView = itemView.findViewById(R.id.folio_id_cot)
        val fechaHoraCita: TextView = itemView.findViewById(R.id.fecha_hora_cita)
        val ingenieroCampo: TextView = itemView.findViewById(R.id.ingeniero_campo)
        val nombre_empresa: TextView = itemView.findViewById(R.id.nombre_empresa)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdmViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_pdm, parent, false)
        return PdmViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PdmViewHolder, position: Int) {
        val currentItem = pdmList[position]
        holder.nombrePdm.text = currentItem.nombre_pdm
        holder.pqAtendera.text = currentItem.pq_atendera
        holder.folioIdCot.text = currentItem.folio_id_cot
        holder.fechaHoraCita.text = currentItem.fecha_hora_cita
        holder.ingenieroCampo.text = currentItem.ingeniero_campo
        holder.nombre_empresa.text = currentItem.nombre_empresa

        holder.itemView.setOnClickListener {
            onClickListener(currentItem)
        }

    }

    override fun getItemCount() = pdmList.size
}
