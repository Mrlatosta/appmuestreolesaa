package com.example.aplicacionlesaa.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.databinding.ItemFisicoquimicosBinding
import com.example.aplicacionlesaa.model.analisisFisico

class analisisFisicoViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val binding = ItemFisicoquimicosBinding.bind(view)

    fun bind(analisisF: analisisFisico, onEdit: (Int) -> Unit) {
        binding.tvRegistroMuestra.text = analisisF.registro_muestra
        binding.tvnombreMuestra.text = analisisF.nombre_muestra
        binding.tvhoraAnalisis.text = analisisF.hora_analisis
        binding.tvtemp.text = analisisF.temperatura

        // Set initial values to EditTexts
        binding.tvPH.text = Editable.Factory.getInstance().newEditable(analisisF.ph)
        binding.tvCLT.text = Editable.Factory.getInstance().newEditable(analisisF.clt?.toString() ?: "")
        binding.tvCLR.text = Editable.Factory.getInstance().newEditable(analisisF.clr?.toString() ?: "")
        binding.tvCRNAS.text = Editable.Factory.getInstance().newEditable(analisisF.crnas?.toString() ?: "")
        binding.tvCYA.text = Editable.Factory.getInstance().newEditable(analisisF.cya?.toString() ?: "")
        binding.tvTUR.text = Editable.Factory.getInstance().newEditable(analisisF.tur?.toString() ?: "")

        // Add TextWatchers to update fields dynamically
        binding.tvPH.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                analisisF.ph = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.tvCLT.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                analisisF.clt = s.toString().toIntOrNull()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.tvCLR.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                analisisF.clr = s.toString().toIntOrNull()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.tvCRNAS.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                analisisF.crnas = s.toString().toIntOrNull()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.tvCYA.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                analisisF.cya = s.toString().toIntOrNull()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.tvTUR.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                analisisF.tur = s.toString().toIntOrNull()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }
}
