package com.example.aplicacionlesaa

import RetrofitClient
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.adapter.PdmAdapter
import com.example.aplicacionlesaa.adapter.servicioAdapter
import com.example.aplicacionlesaa.databinding.ActivitySelePdmBinding
import com.example.aplicacionlesaa.model.ClientePdm
import com.example.aplicacionlesaa.model.Descripcion
import com.example.aplicacionlesaa.model.FolioMuestreo
import com.example.aplicacionlesaa.model.Lugares
import com.example.aplicacionlesaa.model.Pdm
import com.example.aplicacionlesaa.model.Plandemuestreo
import com.example.aplicacionlesaa.model.Servicio
import com.example.aplicacionlesaa.model.UltimoFolio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse
import java.time.LocalDate


class SelePdmActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelePdmBinding
    private val planesList: MutableList<Plandemuestreo> = mutableListOf()
    private val planesDetalladosList: MutableList<Pdm> = mutableListOf()
    private var servicioMutableList: MutableList<Servicio> =
        servicioProvider.listadeServicios.toMutableList()
    private lateinit var adapter: servicioAdapter
    private var clientePdm: ClientePdm? = null
    private var ultimoFolio: UltimoFolio? = null
    private var siguienteFolio: UltimoFolio? = null
    private var lugares: MutableList<Lugares>? = null
    private val descripcionesList: MutableList<Descripcion> = mutableListOf()
    var nombresLugares: MutableList<String> = mutableListOf()


    class servicioProvider {
        companion object{
            val listadeServicios = listOf<Servicio>()

        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySelePdmBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        muestraProvider.listademuestras

        initRecyclerView()




        val spinnerSele = binding.spinnerSelePdm
        val apiService = RetrofitClient.instance
        //Obtener ultimo folio
        apiService.getLastFolioMuestreo().enqueue(object : Callback<UltimoFolio> {
            override fun onResponse(call: Call<UltimoFolio>, response: Response<UltimoFolio>) {
                if (response.isSuccessful) {
                     ultimoFolio = response.body()
                    val siguienteFolio = ultimoFolio?.folio?.toIntOrNull()?.plus(1)?.let {
                        String.format("%06d", it)  // Formatea el número con 6 dígitos, rellenando con ceros a la izquierda si es necesario
                    }
                    binding.tvFolioSiguiente.text = siguienteFolio
                } else {
                    Toast.makeText(this@SelePdmActivity, "Error en la obtencion del ultimo folio", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UltimoFolio>, t: Throwable) {
                Toast.makeText(this@SelePdmActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

        //Obtener descripciones
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
                    Log.e("MainActivity", "Error en Autocomplete: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Descripcion>>, t: Throwable) {
                Log.e("MainActivity", "Failure en autocomplete: ${t.message}")
            }
        })


        //ObtenerPlanes

        apiService.getPlanes().enqueue(object : Callback<List<Plandemuestreo>> {
            override fun onResponse(call: Call<List<Plandemuestreo>>, response: Response<List<Plandemuestreo>>) {
                if (response.isSuccessful) {
                    response.body()?.let { planes ->
                        for (plan in planes) {
                            planesList.addAll(planes)
                            println("La lista de planes es: "+ planesList)
                            val nomplanes = planes.map { it.nombre_pdm.toString() } // Convertir IDs a Strings

                            // Configurar Autocompleteview
                            val adapter = ArrayAdapter(this@SelePdmActivity, android.R.layout.simple_spinner_item,nomplanes )
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinnerSele.adapter = adapter





                        }
                    }
                } else {
                    Log.e("SelePdmActivity", "Error en Spinner: ${response.code()}")
                    Toast.makeText(this@SelePdmActivity, "Error en la conexión", Toast.LENGTH_SHORT).show()

                }
            }

            override fun onFailure(call: Call<List<Plandemuestreo>>, t: Throwable) {
                Log.e("SelePdmActivity", "Failure en autocomplete: ${t.message}")
                Toast.makeText(this@SelePdmActivity, "Error en la conexión", Toast.LENGTH_SHORT).show()
            }
        })

        apiService.getPlanesRecortado().enqueue(object : Callback<List<Pdm>> {
            override fun onResponse(call: Call<List<Pdm>>, response: Response<List<Pdm>>) {
                if (response.isSuccessful) {
                    response.body()?.let { planes ->
                        planesDetalladosList.addAll(planes)
                    }
                } else {
                    Log.e("SelePdmActivity", "Error en pdm: ${response.code()}")

                }
            }

            override fun onFailure(call: Call<List<Pdm>>, t: Throwable) {
                Log.e("SelePdmActivity", "Error: ${t.message}")
            }
        })




        val btnBuscar = binding.btnBuscar
        val txtNombre = binding.txtNombre
        val txtDireccion = binding.txtDireccion
        val txtAtencion = binding.txtAtencion
        val txtPuesto = binding.txtPuesto
        val txtTelefono = binding.txtTelefono
        val txtCorreo = binding.txtCorreo

        btnBuscar.setOnClickListener {
            try {

                var clientId = spinnerSele.selectedItem.toString()

                CoroutineScope(Dispatchers.IO).launch {
                    val response =
                        RetrofitClient.instance.getPlanClienteByPdmName(clientId).awaitResponse()
                    if (response.isSuccessful) {
                        clientePdm = response.body()
                        withContext(Dispatchers.Main) {
                            clientePdm?.let { updateUI(it) }
                            Log.d("SelePdmActivity", "Cliente encontrado: $clientePdm")
                            val call: Call<List<Lugares>> = apiService.getClienteLugarById(clientePdm?.folio.toString())

                            call.enqueue(object : Callback<List<Lugares>> {
                                override fun onResponse(call: Call<List<Lugares>>, response: Response<List<Lugares>>) {
                                    if (response.isSuccessful) {
                                        val lugaresList: List<Lugares>? = response.body()
                                        nombresLugares.clear()

                                        lugaresList?.forEach { lugar ->
                                            nombresLugares.add(lugar.nombre_lugar)
                                        }
                                        Log.d("ApiService", "Lugares: $lugaresList")

                                        // Llamar a la función para iniciar la otra actividad y pasar la lista de nombres
                                    } else {
                                        Log.e("ApiService", "Error en la respuesta: ${response.code()}")
                                    }
                                }

                                override fun onFailure(call: Call<List<Lugares>>, t: Throwable) {
                                    Log.e("ApiService", "Error al realizar la llamada: ${t.message}", t)
                                    // Manejar el error de la llamada aquí
                                }
                            })

                        }
                    } else {
                        Log.e("SelePdmActivity", "Error: ${response.code()}")
                    }
                }



                apiService.getPlanServicesByName(spinnerSele.selectedItem.toString())
                    .enqueue(object : Callback<List<Servicio>> {
                        override fun onResponse(
                            call: Call<List<Servicio>>,
                            response: Response<List<Servicio>>
                        ) {
                            if (response.isSuccessful) {
                                response.body()?.let { servicios ->
                                    servicioMutableList.clear()
                                    servicioMutableList.addAll(servicios)
                                    adapter.notifyDataSetChanged()


                                }
                                /*for (servicio in servicioMutableList) {
                                    if (servicio.descripcion.contains("AGUA DE USO RECREATIVO") || servicio.descripcion.contains("AGUA DE ALBERCA")) {
                                        if (servicio.descripcion.contains("microbiológico") || servicio.descripcion.contains("microbiologico")){
                                            println("Por dos")
                                            servicio.cantidad *= 4
                                        }
                                    }
                                }*/
                                for (servicio in servicioMutableList) {
                                    if (servicio.descripcion.contains("RECOLECCION DE MUESTRAS")){
                                        servicio.cantidad = 0
                                        }
                                    else {
                                        if (servicio.descripcion.contains(
                                                "AGUA DE USO RECREATIVO",
                                                true
                                            ) || servicio.descripcion.contains(
                                                "AGUA DE ALBERCA",
                                                true
                                            )
                                        ) {
                                            if (servicio.estudios_microbiologicos.contains(
                                                    "Ng,Ac",
                                                    true
                                                )
                                            ) {
                                                servicio.estudios_microbiologicos =
                                                    servicio.estudios_microbiologicos.replace(
                                                        "Ng,Ac",
                                                        "Avl"
                                                    )
                                                Log.e(
                                                    "SelePdmActivity",
                                                    "Ng,Ac Encontrado, cambiando"
                                                )
                                            }
                                        }else if(servicio.descripcion.contains("CRUDOS", true)){
                                            servicio.estudios_microbiologicos = "ALIMENTOS CRUDOS"
                                    }else{
                                            servicio.estudios_microbiologicos = extractBeforeHyphen(servicio.descripcion)
                                        }
                                    }
                                }
                            } else {
                                Log.e("MainActivity", "Error: ${response.code()}")
                            }
                        }

                        override fun onFailure(call: Call<List<Servicio>>, t: Throwable) {
                            Log.e("MainActivity", "Failure: ${t.message}")
                        }
                    })

            }catch (e:Exception){
                Toast.makeText(this@SelePdmActivity, "Error al buscar, probablemente no hayas seleccionado ningun pdm", Toast.LENGTH_SHORT).show()
            }


        }

        binding.btnInfo.setOnClickListener {
            showPdmDialog()
        }

        spinnerSele.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                txtCorreo.text = ""
                txtTelefono.text = ""
                txtPuesto.text = ""
                txtAtencion.text = ""
                txtDireccion.text = ""
                txtNombre.text = ""

                servicioMutableList.clear()
                adapter.notifyDataSetChanged()

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Manejar la situación en la que no se ha seleccionado nada en el Spinner (opcional)
            }
        }





        val btnSiguiente = binding.btnSiguiente

        btnSiguiente.setOnClickListener {

            showConfirmationDialog()

        }







    }

    private fun showConfirmationDialog() {
        // Crear y mostrar el cuadro de diálogo de confirmación
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmación")
        builder.setMessage("¿Estás seguro de que quieres crear un Folio de solicitud de servicio?")

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

        val spinnerSele = binding.spinnerSelePdm
        // Realizar la acción deseada aquí
        // Por ejemplo, mostrar un mensaje de que la acción fue realizada
        val intent = Intent(this@SelePdmActivity, MainActivity::class.java)
        val folioSiguiente = binding.tvFolioSiguiente.text.toString()
        val folio_cliente = clientePdm?.folio
        val pdmSelecionado = spinnerSele.selectedItem.toString()
        val folMuestreo = FolioMuestreo(
            folio = folioSiguiente,
            fecha = LocalDate.now().toString(),
            folio_cliente = folio_cliente.toString(),
            folio_pdm = pdmSelecionado
        )

        val pdmDetallado:Pdm? = planesDetalladosList.find { it.nombre_pdm == pdmSelecionado }




        Log.d("SelePdmActivity", "Datos del folio: $folMuestreo")
        sendDataToApi(folMuestreo, object : SendDataCallback {
            override fun onSuccess() {


                // Acción en caso de éxito
                intent.putExtra("plandemuestreo", spinnerSele.selectedItem.toString())
                intent.putParcelableArrayListExtra("listaServicios", ArrayList(servicioMutableList))
                intent.putExtra("clientePdm", clientePdm)
                intent.putExtra("folio", binding.tvFolioSiguiente.text.toString())
                intent.putExtra("pdmDetallado", pdmDetallado)
                Log.e("Aui esto","La lista de lugares es: $nombresLugares")
                intent.putStringArrayListExtra("lugares", ArrayList(nombresLugares))
                intent.putParcelableArrayListExtra("descripciones", ArrayList(descripcionesList))
                Log.i("MainActivity", "Las descripciones son: $descripcionesList.")


                startActivity(intent)
                println(spinnerSele.selectedItem.toString())
                finish()

                Log.d("MainActivity", "Datos enviados exitosamente.")
            }

            override fun onError(message: String) {
                // Acción en caso de error
                Log.e("MainActivity", "Error al enviar datos: $message")
                Toast.makeText(this@SelePdmActivity, "Error al crear el folio", Toast.LENGTH_SHORT).show()
            }
        })



    }

    private fun updateUI(clientePdm: ClientePdm) {
        binding.txtNombre.text = clientePdm.nombre_empresa
        binding.txtDireccion.text = clientePdm.direccion
        binding.txtAtencion.text = clientePdm.atencion
        binding.txtPuesto.text = clientePdm.puesto
        binding.txtTelefono.text = clientePdm.telefono
        binding.txtCorreo.text = clientePdm.correo
    }

    private fun initRecyclerView() {
        //val recyclerView = findViewById<RecyclerView>(R.id.recyclerMuestras)
        //adapter = muestraAdapter(){}
        adapter = servicioAdapter(
            servicioList = servicioMutableList,
            onClickListener = { servicio -> onItemSelected(servicio) },
            onclickDelete = { position -> onDeletedItem(position) })

        binding.recyclerServicios.layoutManager = LinearLayoutManager(this)
        binding.recyclerServicios.adapter = adapter

    }

    private fun onItemSelected(servicio: Servicio) {
        Log.i("Ray", servicio.descripcion)
    }

    private fun onDeletedItem(position: Int) {
        Log.i("Ray", "Funcion eliminar deshabilitada")


    }
    interface SendDataCallback {
        fun onSuccess()
        fun onError(message: String)
    }
    private fun sendDataToApi(folioMuestreo: FolioMuestreo, callback: SendDataCallback) {
        Log.d("sendDataToApi", "Enviando datos: $folioMuestreo")

        val call = RetrofitClient.instance.createFolioMuestreo(folioMuestreo)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("sendDataToApi", "Folio creado correctamente, iniciando Muestreo: ${response.body()}")
                    Toast.makeText(this@SelePdmActivity, "Datos enviados con éxito", Toast.LENGTH_SHORT).show()
                    callback.onSuccess()
                } else {
                    val errorMsg = "Error al enviar datos, código: ${response.code()}, mensaje: ${response.message()}"
                    Log.e("sendDataToApi", errorMsg)
                    Toast.makeText(this@SelePdmActivity, "Error al enviar datos", Toast.LENGTH_SHORT).show()
                    callback.onError(errorMsg)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                val errorMsg = "Error de red: ${t.message}"
                Log.e("sendDataToApi", errorMsg, t)
                Toast.makeText(this@SelePdmActivity, errorMsg, Toast.LENGTH_SHORT).show()
                callback.onError(errorMsg)
            }
        })
    }

    private fun showPdmDialog() {


        val dialogView = layoutInflater.inflate(R.layout.dialog_pdm_list, null)
        val recyclerView: RecyclerView = dialogView.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PdmAdapter(planesDetalladosList)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Lista de planes de muestreo de hoy")
            .setView(dialogView)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()
    }
    fun extractBeforeHyphen(input: String): String {
        val regex = Regex("^(.*?)(\\s-\\s)")
        val matchResult = regex.find(input)
        return matchResult?.groups?.get(1)?.value ?: ""
    }




}