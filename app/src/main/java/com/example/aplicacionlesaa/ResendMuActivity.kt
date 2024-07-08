package com.example.aplicacionlesaa

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplicacionlesaa.adapter.muestraAdapterActResumen
import com.example.aplicacionlesaa.databinding.ActivityResendMuBinding
import com.example.aplicacionlesaa.model.MuestraData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader


class ResendMuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResendMuBinding
    private var muestraData: MuestraData? = null
    private var muestraMutableList: MutableList<Muestra> = mutableListOf()
    private lateinit var adapter: muestraAdapterActResumen
    private val getFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleFile(uri)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_resend_mu)
        binding = ActivityResendMuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnSubir = binding.btnSubir
        btnSubir.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/json"
            }
            muestraData = null  // Limpia muestraData antes de cargar un nuevo archivo
            muestraMutableList?.clear()  // Limpia la lista de muestras
            getFile.launch(intent)



        }


    }

    private fun handleFile(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = InputStreamReader(inputStream)
            val muestraDataType = object : TypeToken<MuestraData>() {}.type
            muestraData = Gson().fromJson(reader, muestraDataType)
            updateUI()

        }
    }

    private fun updateUI() {
        muestraData?.let { data ->
            // Actualiza la UI o realiza las acciones necesarias con muestraData
            Log.e("MuestraData", "MuestraData: $data")
            binding.tvFolio.text = muestraData!!.folio
            binding.tvCliente.text = muestraData!!.clientePdm?.nombre_empresa ?: "Error"
            binding.tvPdm.text = muestraData!!.planMuestreo

            try{
                println("MuestraData.muestras: ${muestraData!!.muestras}")

                for (muestraa in muestraData!!.muestras) {
                    muestraMutableList?.add(muestraa)
                }


                Log.e("MuestraMutableList", "MuestraMutableList: $muestraMutableList")

                initRecyclerView()

                try {
                    adapter.notifyDataSetChanged()
                    Log.e("MuestraMutableList", "MuestraMutableList: $muestraMutableList")
                }catch (e: Exception){
                    Log.e("MuestraData", "Error al cargar las muestras con el notifyData: ${e.message}")
                }

            }catch (e: Exception){
                Log.e("MuestraData", "Error al cargar las muestras: ${e.message}")
            }

            // Aquí puedes agregar más lógica para actualizar la UI con los datos de muestraData
        } ?: run {
            Log.e("MuestraData", "muestraData no ha sido inicializada")
        }
    }

    private fun initRecyclerView() {
        //val recyclerView = findViewById<RecyclerView>(R.id.recyclerMuestras)
        //adapter = muestraAdapter(){}
        adapter = muestraAdapterActResumen(
            muestraList = muestraMutableList!!,
            onClickListener = { muestra -> onItemSelected(muestra) },
            onclickDelete = { position -> onDeletedItem(position) })

        binding.recyclerReenviar.layoutManager = LinearLayoutManager(this)
        binding.recyclerReenviar.adapter = adapter

        println("Exito,Recyler Cargado")

    }
    private fun onItemSelected(muestra: Muestra) {
        Toast.makeText(this, muestra.nombreMuestra, Toast.LENGTH_SHORT).show()
        Log.i("Ray", muestra.nombreMuestra)
    }

    private fun onDeletedItem(position: Int) {
        println("Funcion desactivada")
    }



}