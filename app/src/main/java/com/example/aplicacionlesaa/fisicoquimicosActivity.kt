package com.example.aplicacionlesaa

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.appcompat.app.AlertDialog


class fisicoquimicosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFisicoquimicosBinding
    private lateinit var adapter: analisisFisicoAdapter
    private var analisisFisicoList: MutableList<analisisFisico> = mutableListOf()
    private var datosGuardados = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFisicoquimicosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Configurar el evento hacia atras con los botones del sistema


        // Configurar el reloj
        initClock()

        // Verificar si hay datos guardados previamente
        val savedData = getSavedAnalisisFisicoList()
        if (savedData.isNotEmpty()) {
            // Si ya hay datos guardados, los usamos
            analisisFisicoList = savedData.toMutableList()
        } else {
            // Si no hay datos guardados, inicializamos desde el Intent
            initializeAnalisisFisicoList()
        }

        initRecyclerView()
        val folioSolicitud =  intent.getStringExtra("folioSolicitud") ?: ""
        val nombreCliente = intent.getStringExtra("ClienteNombre") ?: ""
        binding.tvFolioSolicitudFQ.text = "Folio solicitud: $folioSolicitud"
        binding.tvNombreClienteFQ.text = "Cliente: $nombreCliente"

        binding.btnGuardarFQ.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Guardar datos")
            builder.setMessage("¿Desea guardar los datos?")

            builder.setPositiveButton("Sí") { _, _ ->
                // Guardar los datos
                datosGuardados = true
                val datosActualizados = adapter.obtenerDatosActualizados()
                saveAnalisisFisicoList(datosActualizados)
                Log.e("Datos actualizados", "Datos Actualizados: $datosActualizados")
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "No se guardaron los datos", Toast.LENGTH_SHORT).show()
            }
            builder.show()
        }

        binding.btnBorrarFQ.setOnClickListener {
            //Preguntar si si quiere borrar todo
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Borrar datos")
            builder.setMessage("¿Desea borrar todos los datos?")
            builder.setPositiveButton("Sí") { _, _ ->
                // Borrar todos los datos

                borrarTodasLasPreferencias()
                analisisFisicoList.clear()
                adapter.notifyDataSetChanged()
            }
            builder.setNegativeButton("No") { dialog, _ ->

                dialog.dismiss()

                Toast.makeText(this, "No se borraron los datos", Toast.LENGTH_SHORT).show()

            }

            builder.show()


        }




    }

    private fun initClock() {
        val tvhoram = binding.tvContadorHora
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                tvhoram.text = timeFormat.format(Date())
                handler.postDelayed(this, 1000) // Actualiza cada segundo
            }
        }
        handler.post(runnable)
    }

    override fun onBackPressed() {
        if (datosGuardados == false) {
            // Crear un AlertDialog para la confirmación
            AlertDialog.Builder(this).apply {
                setTitle("Confirmación")
                setMessage("¿Estás seguro de que deseas salir, sin guardar los datos?")
                setPositiveButton("Sí") { dialog, _ ->
                    dialog.dismiss()
                    super.onBackPressed() // Llamar al método onBackPressed original
                }
                setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss() // Cerrar el cuadro de diálogo y no hacer nada más
                }
                create()
                show()
            }
            } else {
                super.onBackPressed() // Llamar al método onBackPressed original
            Toast.makeText(this, "Se han guardado los datos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeAnalisisFisicoList() {
        val muestraMutableList = intent.getParcelableArrayListExtra<Muestra>("muestraList") ?: mutableListOf()
        val serviciosList = intent.getParcelableArrayListExtra<Servicio>("listaServicios") ?: mutableListOf()
        Log.i("Inicializando","Inicializando Lista")
        Log.i("MuestrASIZE","Muestra size ${muestraMutableList.size}")
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
        Log.e("Lista inicial", "analisisFisicoList: $analisisFisicoList")
    }

    private fun initRecyclerView() {
        adapter = analisisFisicoAdapter(analisisFisicoList) { position ->
            // Aquí puedes manejar acciones de edición si es necesario
        }
        binding.recyclerFisicoquimicos.layoutManager = LinearLayoutManager(this)
        binding.recyclerFisicoquimicos.adapter = adapter
    }

    private fun getSavedAnalisisFisicoList(): List<analisisFisico> {
        val sharedPreferences = getSharedPreferences("FisicoquimicosPrefs", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("analisisFisicoList", null)
        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<analisisFisico>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    private fun saveAnalisisFisicoList(lista: List<analisisFisico>) {
        val sharedPreferences = getSharedPreferences("FisicoquimicosPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(lista)
        editor.putString("analisisFisicoList", json)
        editor.apply()
        Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
    }

    private fun borrarTodasLasPreferencias() {

        val sharedPreferences = getSharedPreferences("FisicoquimicosPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() // Borra todas las claves y valores
        editor.apply() // Aplica los cambios
    }

}
