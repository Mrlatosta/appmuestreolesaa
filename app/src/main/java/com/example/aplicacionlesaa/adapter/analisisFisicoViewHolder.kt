package com.example.aplicacionlesaa.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.databinding.ItemFisicoquimicosBinding
import com.example.aplicacionlesaa.model.analisisFisico

class analisisFisicoViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val binding = ItemFisicoquimicosBinding.bind(view)

    private var currentItem: analisisFisico? = null

    init {
        binding.tvPH.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                currentItem?.let {
                    val phText = s.toString()
                    it.ph = phText

                    val phValue = phText.toFloatOrNull()
                    if (phText.isNotEmpty()) {
                        if (phValue == null) {
                            binding.tvPH.error = "Formato inválido"
                        } else if (phValue < 1 || phValue > 14) {
                            binding.tvPH.error = "El pH debe estar entre 1 y 14 (P/Alb: 6.5 - 8.5)"
                        } else {
                            binding.tvPH.error = null
                        }
                    } else {
                        binding.tvPH.error = "Este campo es obligatorio"
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.tvCLT.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                currentItem?.let {
                    it.clt = s.toString().toBigDecimalOrNull()
                    actualizarCRNAS(it)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.tvCLR.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                currentItem?.let {
                    it.clr = s.toString().toBigDecimalOrNull()
                    actualizarCRNAS(it)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.tvCYA.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                currentItem?.cya = s.toString().toBigDecimalOrNull()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.tvTUR.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                currentItem?.tur = s.toString().toBigDecimalOrNull()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    fun bind(analisisF: analisisFisico, onEdit: (Int) -> Unit) {
        currentItem = analisisF

        binding.tvRegistroMuestra.text = analisisF.registro_muestra
        binding.tvnombreMuestra.text = analisisF.nombre_muestra
        binding.tvhoraAnalisis.text = analisisF.hora_analisis
        binding.tvtemp.text = analisisF.temperatura

        // Al actualizar texto debemos deshabilitar temporalmente el listener para evitar loops
        binding.tvPH.setText(analisisF.ph)
        binding.tvCLT.setText(analisisF.clt?.toPlainString() ?: "")
        binding.tvCLR.setText(analisisF.clr?.toPlainString() ?: "")
        binding.tvCRNAS.setText(analisisF.crnas?.toPlainString() ?: "")
        binding.tvCYA.setText(analisisF.cya?.toPlainString() ?: "")
        binding.tvTUR.setText(analisisF.tur?.toPlainString() ?: "")
    }

    private fun actualizarCRNAS(analisisF: analisisFisico) {
        val cltValue = binding.tvCLT.text.toString().toBigDecimalOrNull()
        val clrValue = binding.tvCLR.text.toString().toBigDecimalOrNull()

        if (cltValue != null && clrValue != null) {
            if (cltValue < clrValue) {
                binding.tvCLR.error = "CLR no puede ser mayor que CLT"
                binding.tvCLT.error = "CLT no puede ser menor que CLR"
                binding.tvCRNAS.setText("")
                analisisF.crnas = null
            } else {
                binding.tvCLR.error = null
                binding.tvCLT.error = null
                val resta = cltValue - clrValue
                binding.tvCRNAS.setText(resta.toPlainString())
                analisisF.crnas = resta
            }
        } else {
            binding.tvCLR.error = null
            binding.tvCLT.error = null
            binding.tvCRNAS.setText("")
            analisisF.crnas = null
        }
    }
}

