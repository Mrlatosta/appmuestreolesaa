package com.example.aplicacionlesaa

import android.content.Intent
import android.os.Bundle
import android.text.Editable
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
import com.example.aplicacionlesaa.adapter.muestraAdapter
import com.example.aplicacionlesaa.adapter.servicioAdapter
import com.example.aplicacionlesaa.databinding.ActivitySelePdmBinding
import com.example.aplicacionlesaa.model.ClientePdm
import com.example.aplicacionlesaa.model.Descripcion
import com.example.aplicacionlesaa.model.FolioMuestreo
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
    private var servicioMutableList: MutableList<Servicio> =
        servicioProvider.listadeServicios.toMutableList()
    private lateinit var adapter: servicioAdapter
    private var clientePdm: ClientePdm? = null
    private var ultimoFolio: UltimoFolio? = null
    private var siguienteFolio: UltimoFolio? = null


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

                                    servicioMutableList.addAll(servicios)
                                    adapter.notifyDataSetChanged()


                                }
                                for (servicio in servicioMutableList) {
                                    if (servicio.descripcion.contains("AGUA DE USO RECREATIVO") || servicio.descripcion.contains("AGUA DE ALBERCA")) {
                                        if (servicio.estudios_fisicoquimicos == "*"){
                                            println("Por dos")
                                            servicio.cantidad *= 2
                                        }else{
                                            servicio.cantidad *= 3
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

        Log.d("SelePdmActivity", "Datos del folio: $folMuestreo")
        sendDataToApi(folMuestreo, object : SendDataCallback {
            override fun onSuccess() {
                // Acción en caso de éxito
                intent.putExtra("plandemuestreo", spinnerSele.selectedItem.toString())
                intent.putParcelableArrayListExtra("listaServicios", ArrayList(servicioMutableList))
                intent.putExtra("clientePdm", clientePdm)
                intent.putExtra("folio", binding.tvFolioSiguiente.text.toString())


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



}