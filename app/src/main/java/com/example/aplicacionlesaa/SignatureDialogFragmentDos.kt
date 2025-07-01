package com.example.aplicacionlesaa

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment

class SignatureDialogFragmentDos : DialogFragment() {

    private var signatureView: SignatureView? = null
    private var listener: SignatureDialogListener? = null

    interface SignatureDialogListener {
        fun onSignatureSavedDos(bitmap: Bitmap)
    }

    fun setSignatureDialogListenerDos(listener: SignatureDialogListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signature_dialog, container, false)
        signatureView = view.findViewById(R.id.signatureView)
        val btnSave: Button = view.findViewById(R.id.btnSave)
        val btnClear: Button = view.findViewById(R.id.btnClear)

        btnSave.setOnClickListener {
            signatureView?.let {
                val originalBitmap = it.getSignatureBitmap()

                // Escalamos al tamaño deseado
                val targetWidth = 282  // ancho en dp del SignatureView pequeño
                val targetHeight = 96  // alto en dp

                // Convertir dp a pixels:
                val scale = resources.displayMetrics.density
                val targetWidthPx = (targetWidth * scale).toInt()
                val targetHeightPx = (targetHeight * scale).toInt()

                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidthPx, targetHeightPx, true)


                listener?.onSignatureSavedDos(scaledBitmap)
                dismiss()
            }
        }

        btnClear.setOnClickListener {
            signatureView?.clear()
        }

        return view
    }
}
