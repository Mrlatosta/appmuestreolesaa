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
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aplicacionlesaa.adapter.muestraAdapterActResumen
import com.example.aplicacionlesaa.databinding.ActivityResendMuBinding
import com.example.aplicacionlesaa.model.MuestraData
import com.example.aplicacionlesaa.model.Muestra_pdm
import com.example.aplicacionlesaa.utils.NetworkUtils
import com.example.aplicacionlesaa.worker.SendDataWorker
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

        val btnReenviar = binding.btnReenviar
        btnReenviar.setOnClickListener {
            val muestraListaNueva = convertirAMuestraPdm(muestraMutableList)

            if (NetworkUtils.isInternetAvailable(this)) {
                Log.i("Internet", "Si hay internet")



                Toast.makeText(this, "Si hay internet, enviando muestras", Toast.LENGTH_SHORT).show()
                val tamaño = muestraListaNueva.size

                // Crear una lista de Data para cada muestra en muestraMutableList
                val dataList = mutableListOf<Data>()
                //Envio de lista de muestra
                muestraListaNueva.forEachIndexed { index, muestra ->
                    val data = Data.Builder()
                        .putInt("muestra_count",tamaño)
                        .putString("registro_muestra_$index", muestra.registro_muestra)
                        .putString("folio_muestreo_$index", muestra.folio_muestreo)
                        .putString("fecha_muestreo_$index", muestra.fecha_muestreo)
                        .putString("nombre_muestra_$index", muestra.nombre_muestra)
                        .putString("id_lab_$index", muestra.id_lab)
                        .putString("cantidad_aprox_$index", muestra.cantidad_aprox)
                        .putString("temperatura_$index", muestra.temperatura)
                        .putString("lugar_toma_$index", muestra.lugar_toma)
                        .putString("descripcion_toma_$index", muestra.descripcion_toma)
                        .putString("e_micro_$index", muestra.e_micro)
                        .putString("e_fisico_$index", muestra.e_fisico)
                        .putString("observaciones_$index", muestra.observaciones)
                        .putString("folio_pdm_$index", muestra.folio_pdm)
                        .putInt("servicio_id_$index", muestra.servicio_id)
                        .build()

                    dataList.add(data)
                }

                // Crear y enviar las tareas programadas para cada muestra en muestraMutableList
                dataList.forEach { data ->
                    val workRequest = OneTimeWorkRequestBuilder<SendDataWorker>()
                        .setInputData(data)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()

                    Log.i("Si hay internet", "Entre al worker")
                    WorkManager.getInstance(this).enqueue(workRequest)
                }

                /*enqueueSendEmailTask(this, "atencionaclienteslab.lesa@gmail.com",
                    "$pdfPath/$nombreArchivoPdf")*/
            } else {
                Toast.makeText(this, "No hay internet, los datos se enviarán cuando se establezca una conexión", Toast.LENGTH_SHORT).show()
                Log.i("Internet", "No hay internet")
                val tamaño = muestraListaNueva.size

                // Crear una lista de Data para cada muestra en muestraMutableList
                val dataList = mutableListOf<Data>()
                muestraListaNueva.forEachIndexed { index, muestra ->
                    val data = Data.Builder()
                        .putInt("muestra_count",tamaño)
                        .putString("registro_muestra_$index", muestra.registro_muestra)
                        .putString("folio_muestreo_$index", muestra.folio_muestreo)
                        .putString("fecha_muestreo_$index", muestra.fecha_muestreo)
                        .putString("nombre_muestra_$index", muestra.nombre_muestra)
                        .putString("id_lab_$index", muestra.id_lab)
                        .putString("cantidad_aprox_$index", muestra.cantidad_aprox)
                        .putString("temperatura_$index", muestra.temperatura)
                        .putString("lugar_toma_$index", muestra.lugar_toma)
                        .putString("descripcion_toma_$index", muestra.descripcion_toma)
                        .putString("e_micro_$index", muestra.e_micro)
                        .putString("e_fisico_$index", muestra.e_fisico)
                        .putString("observaciones_$index", muestra.observaciones)
                        .putString("folio_pdm_$index", muestra.folio_pdm)
                        .putInt("servicio_id_$index", muestra.servicio_id)
                        .build()

                    dataList.add(data)
                }

                // Crear y enviar las tareas programadas para cada muestra en muestraMutableList
                dataList.forEach { data ->
                    val workRequest = OneTimeWorkRequestBuilder<SendDataWorker>()
                        .setInputData(data)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()

                    Log.i("Datos", "Entre al worker")
                    WorkManager.getInstance(this).enqueue(workRequest)
                }

            }
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

    fun convertirAMuestraPdm(muestras: List<Muestra>): List<Muestra_pdm> {
        val listaMuestrasPdm = mutableListOf<Muestra_pdm>()

        for (muestra in muestras) {
            val muestraPdm = Muestra_pdm(
                registro_muestra = muestra.registroMuestra,
                folio_muestreo = binding.tvFolio.text.toString(),
                fecha_muestreo = muestra.fechaMuestra,
                nombre_muestra = muestra.nombreMuestra,
                id_lab = muestra.idLab,
                cantidad_aprox = muestra.cantidadAprox,
                temperatura = muestra.tempM,
                lugar_toma = muestra.lugarToma,
                descripcion_toma = muestra.descripcionM,
                e_micro = muestra.emicro,
                e_fisico = muestra.efisico,
                observaciones = muestra.observaciones,
                folio_pdm = binding.tvPdm.text.toString(),
                servicio_id = muestra.servicioId
            )
            listaMuestrasPdm.add(muestraPdm)
        }

        return listaMuestrasPdm
    }


}