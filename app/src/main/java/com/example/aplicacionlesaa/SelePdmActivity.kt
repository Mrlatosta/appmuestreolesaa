package com.example.aplicacionlesaa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplicacionlesaa.adapter.muestraAdapter
import com.example.aplicacionlesaa.adapter.servicioAdapter
import com.example.aplicacionlesaa.databinding.ActivitySelePdmBinding
import com.example.aplicacionlesaa.model.ClientePdm
import com.example.aplicacionlesaa.model.Descripcion
import com.example.aplicacionlesaa.model.Plandemuestreo
import com.example.aplicacionlesaa.model.Servicio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse


class SelePdmActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelePdmBinding
    private val planesList: MutableList<Plandemuestreo> = mutableListOf()
    private var servicioMutableList: MutableList<Servicio> =
        servicioProvider.listadeServicios.toMutableList()
    private lateinit var adapter: servicioAdapter


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
                }
            }

            override fun onFailure(call: Call<List<Plandemuestreo>>, t: Throwable) {
                Log.e("SelePdmActivity", "Failure en autocomplete: ${t.message}")
            }
        })

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
                }
            }

            override fun onFailure(call: Call<List<Plandemuestreo>>, t: Throwable) {
                Log.e("SelePdmActivity", "Failure en autocomplete: ${t.message}")
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

            var clientId = spinnerSele.selectedItem.toString()

            CoroutineScope(Dispatchers.IO).launch {
                val response =
                    RetrofitClient.instance.getPlanClienteByPdmName(clientId).awaitResponse()
                if (response.isSuccessful) {
                    val clientePdm = response.body()
                    withContext(Dispatchers.Main) {
                        clientePdm?.let { updateUI(it) }

                    }
                } else {
                    Log.e("SelePdmActivity", "Error: ${response.code()}")
                }
            }
            apiService.getPlanServicesByName(spinnerSele.selectedItem.toString()).enqueue(object : Callback<List<Servicio>> {
                override fun onResponse(call: Call<List<Servicio>>, response: Response<List<Servicio>>) {
                    if (response.isSuccessful) {
                        response.body()?.let { servicios ->
                                servicioMutableList.addAll(servicios)
                                adapter.notifyDataSetChanged()

                        }
                    } else {
                        Log.e("MainActivity", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<List<Servicio>>, t: Throwable) {
                    Log.e("MainActivity", "Failure: ${t.message}")
                }
            })

        }

        val btnSiguiente = binding.btnSiguiente

        btnSiguiente.setOnClickListener {
            val intent = Intent(this@SelePdmActivity, MainActivity::class.java)
            intent.putExtra("plandemuestreo", spinnerSele.selectedItem.toString())
            startActivity(intent)
            println(spinnerSele.selectedItem.toString())
            //finish()
        }







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


}