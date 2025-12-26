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
        val observacionesPDMlista: TextView = itemView.findViewById(R.id.observacionesPDMlista)
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
        //FOrmatear bien la hecha hora cita esta como: 2025-03-17T13:00:00.000Z

        val fechaHoraCita = currentItem.fecha_hora_cita
        val partes = fechaHoraCita.split("T")
        val fecha = partes[0]
        val hora = partes[1]

        holder.fechaHoraCita.text = "$fecha $hora"
        holder.ingenieroCampo.text = currentItem.ingeniero_campo
        holder.nombre_empresa.text = currentItem.nombre_empresa
        holder.observacionesPDMlista.text = currentItem.observaciones


        holder.itemView.setOnClickListener {
            onClickListener(currentItem)
        }

    }

    override fun getItemCount() = pdmList.size
}
