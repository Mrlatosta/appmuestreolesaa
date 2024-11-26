package com.example.aplicacionlesaa

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplicacionlesaa.adapter.analisisFisicoAdapter
import com.example.aplicacionlesaa.adapter.muestraAdapter
import com.example.aplicacionlesaa.databinding.ActivityFisicoquimicosBinding
import com.example.aplicacionlesaa.databinding.ActivityMainBinding
import com.example.aplicacionlesaa.model.Servicio
import com.example.aplicacionlesaa.model.analisisFisico
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class fisicoquimicosActivity : AppCompatActivity() {


    private lateinit var binding: ActivityFisicoquimicosBinding
    private lateinit var muestraMutableList: MutableList<Muestra>
    private val serviciosList: MutableList<Servicio> = mutableListOf()
    private val analisisFisicoList: MutableList<analisisFisico> = mutableListOf()
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var adapter: analisisFisicoAdapter






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        setContentView(R.layout.activity_fisicoquimicos)
        binding = ActivityFisicoquimicosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvhoram = binding.tvContadorHora
        //Crear reloj en un textview que contenga la hora actual
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val currentTime = timeFormat.format(Date())
                tvhoram.text = currentTime
                handler.postDelayed(this, 1000) // Actualiza cada segundo
            }
        }

        // Iniciar la actualización de la hora
        handler.post(runnable)

        //Pasar muestra mutable list para fisicoquimicos
        muestraMutableList = intent.getParcelableArrayListExtra("muestraList") ?: mutableListOf()

        val serviciosRecibidos = intent.getParcelableArrayListExtra<Servicio>("listaServicios")
        if (serviciosRecibidos != null) {
            serviciosList.addAll(serviciosRecibidos)
        }

        Log.e("Lista de servicios","serviciosList: $serviciosList")
        Log.e("Lista de muestras","muestraMutableList: $muestraMutableList")

        for (muestra in muestraMutableList) {
            //Encontrar el servicio correspondiente a la muestra
            val servicio = serviciosList.find { it.id == muestra.servicioId }


            try{
                if (servicio != null) {
                    val descripcion = servicio.descripcion
                    val regex = Regex("agua de alberca", RegexOption.IGNORE_CASE)
                    val regexFisico = Regex("físicoquímico|fisicoquimico|físico-químico", RegexOption.IGNORE_CASE)

                    if (regex.containsMatchIn(descripcion) && regexFisico.containsMatchIn(descripcion)) {
                        //SI si encuentra, entonces crear un nuevo objeto analisisFisico y añadirle su registro muestra del que se esta trabajando, las demas en null
                        Log.e("Muestra encontrada","Muestra encontrada: $muestra")
                        Log.e("Hora actual","Hora actual: ${binding.tvContadorHora.text.toString()}")
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val currentTime = timeFormat.format(Date())

                        val analisisFisico = analisisFisico(
                            registro_muestra = muestra.registroMuestra,
                            nombre_muestra = muestra.lugarToma,
                            //Obtener hora actual
                            hora_analisis = currentTime,
                            temperatura = muestra.tempM,
                            ph = "",
                            clr= null,
                            clt= null,
                            crnas= null,
                            cya=  null,
                            tur= null,

                        )
                        analisisFisicoList.add(analisisFisico)
                    }
                }
            }catch (e:Exception){
                Log.e("Error", "Error al establecer el nombre en txtNombre")
            }

            Log.e("Lista de analisisFisico","analisisFisicoList: $analisisFisicoList")




        }

        initRecyclerView()




    }
    private fun initRecyclerView() {
        //val recyclerView = findViewById<RecyclerView>(R.id.recyclerMuestras)
        //adapter = muestraAdapter(){}
        adapter = analisisFisicoAdapter(
            analisisfisico = analisisFisicoList,
            onclickEdit = { position -> onEditItem(position) }
            )

        binding.recyclerFisicoquimicos.layoutManager = LinearLayoutManager(this)
        binding.recyclerFisicoquimicos.adapter = adapter



    }

    private fun onEditItem(position: Int) {
        Toast.makeText(this, "Editar item $position", Toast.LENGTH_SHORT).show()
    }


}