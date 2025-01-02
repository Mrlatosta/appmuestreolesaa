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
        // Configurar valores iniciales en los EditText
        binding.tvRegistroMuestra.text = analisisF.registro_muestra
        binding.tvnombreMuestra.text = analisisF.nombre_muestra
        binding.tvhoraAnalisis.text = analisisF.hora_analisis
        binding.tvtemp.text = analisisF.temperatura

        binding.tvPH.text = Editable.Factory.getInstance().newEditable(analisisF.ph)
        binding.tvCLT.text = Editable.Factory.getInstance().newEditable(analisisF.clt?.toString() ?: "")
        binding.tvCLR.text = Editable.Factory.getInstance().newEditable(analisisF.clr?.toString() ?: "")
        binding.tvCRNAS.text = Editable.Factory.getInstance().newEditable(analisisF.crnas?.toString() ?: "")
        binding.tvCYA.text = Editable.Factory.getInstance().newEditable(analisisF.cya?.toString() ?: "")
        binding.tvTUR.text = Editable.Factory.getInstance().newEditable(analisisF.tur?.toString() ?: "")

        // Remover los TextWatcher anteriores antes de añadir nuevos
        binding.tvPH.removeTextChangedListener(binding.tvPH.tag as? TextWatcher)
        binding.tvCLT.removeTextChangedListener(binding.tvCLT.tag as? TextWatcher)
        binding.tvCLR.removeTextChangedListener(binding.tvCLR.tag as? TextWatcher)
        binding.tvCRNAS.removeTextChangedListener(binding.tvCRNAS.tag as? TextWatcher)
        binding.tvCYA.removeTextChangedListener(binding.tvCYA.tag as? TextWatcher)
        binding.tvTUR.removeTextChangedListener(binding.tvTUR.tag as? TextWatcher)

        // Agregar nuevos TextWatcher y guardar referencia en la etiqueta (tag)
        val phWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                analisisF.ph = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.tvPH.addTextChangedListener(phWatcher)
        binding.tvPH.tag = phWatcher

        val cltWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                analisisF.clt = s.toString().toBigDecimalOrNull()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.tvCLT.addTextChangedListener(cltWatcher)
        binding.tvCLT.tag = cltWatcher

        val clrWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                analisisF.clr = s.toString().toBigDecimalOrNull()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.tvCLR.addTextChangedListener(clrWatcher)
        binding.tvCLR.tag = clrWatcher

        val crnasWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                analisisF.crnas = s.toString().toBigDecimalOrNull()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.tvCRNAS.addTextChangedListener(crnasWatcher)
        binding.tvCRNAS.tag = crnasWatcher

        val cyaWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                analisisF.cya = s.toString().toBigDecimalOrNull()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.tvCYA.addTextChangedListener(cyaWatcher)
        binding.tvCYA.tag = cyaWatcher

        val turWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                analisisF.tur = s.toString().toBigDecimalOrNull()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.tvTUR.addTextChangedListener(turWatcher)
        binding.tvTUR.tag = turWatcher
    }
}

