package com.example.aplicacionlesaa

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplicacionlesaa.SelePdmActivity.SendDataCallback
import com.example.aplicacionlesaa.adapter.muestraAdapterActResumen
import com.example.aplicacionlesaa.databinding.ActivityContinuarFolioBinding
import com.example.aplicacionlesaa.databinding.ActivityResendMuBinding
import com.example.aplicacionlesaa.model.Descripcion
import com.example.aplicacionlesaa.model.FolioMuestreo
import com.example.aplicacionlesaa.model.MuestraData
import com.example.aplicacionlesaa.model.Pdm
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDate

class ContinuarFolioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContinuarFolioBinding
    private var muestraData: MuestraData? = null
    private var muestraMutableList: MutableList<Muestra> = mutableListOf()
    private var muestrasExtras: MutableList<Muestra> = mutableListOf()
    private lateinit var adapter: muestraAdapterActResumen
    private val getFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleFile(uri)
            }
        }
    }

    private val descripcionesList: MutableList<Descripcion> = mutableListOf()
    private val foliosHoy: MutableList<String> = mutableListOf()
    private val muestrasMap: MutableMap<String, MuestraData> = mutableMapOf()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityContinuarFolioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cargarArchivosAutomaticamente()

        val apiService = RetrofitClient.instance
        apiService.getDescriptions().enqueue(object : Callback<List<Descripcion>> {
            override fun onResponse(
                call: Call<List<Descripcion>>,
                response: Response<List<Descripcion>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { descripciones ->

                        descripcionesList.addAll(descripciones)
                        println("La lista de descripciones es: " + descripcionesList)
//                        val descris =
//                            descripciones.map { it.descripcion.toString() } // Convertir IDs a Strings
//                        // Configurar Autocompleteview
//                        val adapter = ArrayAdapter(
//                            this@SelePdmActivity,
//                            android.R.layout.simple_spinner_dropdown_item,
//                            descris
//                        )
                    }
                } else {
                    Log.e("ContiFolActivity", "Error en Autocomplete: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Descripcion>>, t: Throwable) {
                Log.e("ContiFolActivity", "Failure en autocomplete: ${t.message}")
            }
        })




//        val btnSubir = binding.btnSubir
//        btnSubir.setOnClickListener {
//            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
//                type = "application/json"
//            }
//            muestraData = null  // Limpia muestraData antes de cargar un nuevo archivo
//            muestraMutableList?.clear()  // Limpia la lista de muestras
//            muestrasExtras?.clear()
//            getFile.launch(intent)
//        }
        var spnSeleFolio = binding.spnSeleFolio

        val adapterSpnSele = ArrayAdapter(this@ContinuarFolioActivity, android.R.layout.simple_spinner_item, foliosHoy)
        adapterSpnSele.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnSeleFolio.adapter = adapterSpnSele



        val btnContinuar = binding.btnContinuar
        btnContinuar.setOnClickListener {

            showConfirmationDialog()

        }


    }

    private fun showConfirmationDialog() {
        // Crear y mostrar el cuadro de diálogo de confirmación
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmación")
        builder.setMessage("¿Estás seguro de que quieres continuar un Folio de solicitud de servicio?")

        // Configurar el botón "Sí"
        builder.setPositiveButton("Sí") { dialog, which ->
            performAction()

        }

        // Configurar el botón "No"
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }

        // Mostrar el cuadro de diálogo
        builder.show()
    }

    private fun performAction() {

        var sepudo = false

        // Realizar la acción deseada aquí
        // Por ejemplo, mostrar un mensaje de que la acción fue realizada
        val intent = Intent(this@ContinuarFolioActivity, MainActivity::class.java)
        val pdmSelecionado = muestraData?.planMuestreo
        val folMuestreo = FolioMuestreo(
            folio = muestraData!!.folio,
            fecha = LocalDate.now().toString(),
            folio_cliente = muestraData!!.clientePdm!!.folio,
            folio_pdm = pdmSelecionado!!
        )

        val pdmDetallado: Pdm? = muestraData?.pdmDetallado

        Log.d("ContiFolPDM", "Datos del folio: $folMuestreo")
        intent.putExtra("plandemuestreo", muestraData?.planMuestreo)
        intent.putParcelableArrayListExtra("listaServicios", ArrayList(muestraData!!.serviciosPdm))
        intent.putExtra("clientePdm", muestraData?.clientePdm)
        intent.putExtra("folio", muestraData?.folio)
        intent.putExtra("pdmDetallado", pdmDetallado)
        intent.putParcelableArrayListExtra("muestraList", ArrayList(muestraMutableList))
        intent.putParcelableArrayListExtra("muestraExtraList", ArrayList(muestrasExtras))
        intent.putExtra("tipomuestreo","continuar")
        Log.i("ContinuarFolioActivity", "Las descripciones rayasd son: $descripcionesList.")
        intent.putParcelableArrayListExtra("descripciones", ArrayList(descripcionesList))


        val nombresLugares: MutableList<String> = mutableListOf()

        Log.e("Aui esto","La lista de lugares es: $nombresLugares")
        intent.putStringArrayListExtra("lugares", ArrayList(nombresLugares))
        startActivity(intent)
        finish()

        Log.d("ContinuarFolioActivity", "Datos enviados exitosamente.")



    }

    private fun handleFile(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = InputStreamReader(inputStream)
            val muestraDataType = object : TypeToken<MuestraData>() {}.type
            muestraData = Gson().fromJson(reader, muestraDataType)
            muestraData?.let { data ->
                foliosHoy.add(data.folio)
                muestrasMap[data.folio] = data
                actualizarSpinnerFolios()
            }
        }
    }

    private fun actualizarSpinnerFolios() {
        val adapterSpnSele = ArrayAdapter(this, android.R.layout.simple_spinner_item, foliosHoy)
        adapterSpnSele.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnSeleFolio.adapter = adapterSpnSele
        binding.spnSeleFolio.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val folioSeleccionado = foliosHoy[position]
                muestraData = muestrasMap[folioSeleccionado]
                updateUI()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }


    private fun updateUI() {
        muestraData?.let { data ->

            Log.e("MuestraData", "Las las muestras extras:")

            // Actualiza la UI o realiza las acciones necesarias con muestraData
            Log.e("MuestraData", "MuestraData: $data")
            binding.tvFolio.text = muestraData!!.folio
            binding.tvCliente.text = muestraData!!.clientePdm?.nombre_empresa ?: "Error"
            binding.tvPdm.text = muestraData!!.planMuestreo

            //Limpiar las listas
            muestraMutableList?.clear()
            muestrasExtras?.clear()


            try{
                println("MuestraData.muestras: ${muestraData!!.muestras}")

                for (muestraa in muestraData!!.muestras) {
                    muestraMutableList?.add(muestraa)
                }

                try{
                for (muestrae in muestraData!!.muestrasExtra){
                    muestrasExtras.add(muestrae)
                }

                muestrasExtras?.let { list ->
                    for (muestra in list) {
                        Log.e("Muestra", muestra.toString())
                    }
                    binding.tvMuestaEta.text = "Muestras Extra: " + list.size.toString()
                }
                    }catch (e: Exception){
                    Log.e("MuestraData", "Error al cargar las muestras extras: ${e.message}")
                }


                Log.e("MuestraMutableList", "MuestraMutableList: $muestraMutableList")
                Log.e("MuestraExtraList", "MuestraExtraList: $muestrasExtras")

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

    private fun cargarArchivosAutomaticamente() {
        val directorio = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

        if (!directorio.exists()) {
            Log.e("CargaAutomatica", "El directorio no existe.")
            return
        }

        val fechaHoy = LocalDate.now().toString()
        val archivosHoy = directorio.listFiles { file ->
            file.extension == "json" && file.name.contains(fechaHoy)
        }

        if (!archivosHoy.isNullOrEmpty()) {
            for (archivo in archivosHoy) {
                val uri = Uri.fromFile(archivo)
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = InputStreamReader(inputStream)
                    val muestraDataType = object : TypeToken<MuestraData>() {}.type
                    val data: MuestraData = Gson().fromJson(reader, muestraDataType)
                    foliosHoy.add(data.folio)
                    muestrasMap[data.folio] = data
                    Log.e("CargaAutomatica", "Archivo cargado: $data")
                    //Ver si hay muestras extras
                    Log.e("CargaAutomatica", "Muestras extras: ${data.muestrasExtra}")
                }
            }
            actualizarSpinnerFolios()
        } else {
            Log.e("CargaAutomatica", "No hay archivos JSON para hoy.")
        }
    }




}