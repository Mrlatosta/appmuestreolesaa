package com.example.aplicacionlesaa

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplicacionlesaa.adapter.muestraAdapterActResumen
import com.example.aplicacionlesaa.databinding.ActivityVerMuestrasExtraBinding
import com.example.aplicacionlesaa.model.ClientePdm
import com.example.aplicacionlesaa.model.Pdm


class VerMuestrasExtraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerMuestrasExtraBinding
    private var muestrasExtra: MutableList<Muestra> = mutableListOf()
    private lateinit var adapter: muestraAdapterActResumen
    private var clientePdm: ClientePdm? = null
    private var pdmSeleccionado: String = ""
    private var folio: String? = null
    private lateinit var pdmDetallado: Pdm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityVerMuestrasExtraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        clientePdm = intent.getParcelableExtra("clientePdm")
        pdmSeleccionado = intent.getStringExtra("plandemuestreo").toString()
        folio = intent.getStringExtra("folio")
        pdmDetallado = intent.getParcelableExtra("pdmDetallado")!!

        try{
            val muestrasExtrasMain = intent.getParcelableArrayListExtra<Muestra>("muestraExtraList")
            if (muestrasExtrasMain != null) {
                muestrasExtra.addAll(muestrasExtrasMain)
                initRecyclerView()
            }
        }catch (e: Exception){
            Log.e("Error Mextra", e.message.toString())
        }


        binding.tvCliente.text = clientePdm?.nombre_empresa
        binding.tvFolio.text = folio+"E"
        binding.tvPDM.text = pdmSeleccionado


        binding.btnAceptar.setOnClickListener {
            finish()
        }

    }

    private fun initRecyclerView() {
        //val recyclerView = findViewById<RecyclerView>(R.id.recyclerMuestras)
        //adapter = muestraAdapter(){}
        adapter = muestraAdapterActResumen(
            muestraList = muestrasExtra,
            onClickListener = { muestra -> onItemSelected(muestra) },
            onclickDelete = { position -> onDeletedItem(position) })

        binding.recyclerResumenExtra.layoutManager = LinearLayoutManager(this)
        binding.recyclerResumenExtra.adapter = adapter

    }
    private fun onItemSelected(muestra: Muestra) {
        Toast.makeText(this, muestra.nombreMuestra, Toast.LENGTH_SHORT).show()
        Log.i("Ray", muestra.nombreMuestra)
    }

    private fun onDeletedItem(position: Int) {
        println("Funcion desactivada")
    }


}