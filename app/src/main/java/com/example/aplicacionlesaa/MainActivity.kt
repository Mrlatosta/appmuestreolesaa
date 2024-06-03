package com.example.aplicacionlesaa

import android.os.Bundle
import android.os.Looper
import android.os.Handler
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplicacionlesaa.adapter.muestraAdapter
import com.example.aplicacionlesaa.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    //Lista a la cual le vamos a quitar o poner muestras
    private var muestraMutableList: MutableList<Muestra> =
        muestraProvider.listademuestras.toMutableList()
    private lateinit var adapter: muestraAdapter

    var contador = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Al arrancar la pantalla
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvfecham = binding.tvfechamuestreo
        val tvhoram = binding.tvHora
        val tvFolio = binding.tvFolio

        // Establecer la fecha actual
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        tvfecham.text = currentDate

        // Configurar el handler para actualizar la hora
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


        val tvRegM = binding.tvregistromuestra
        val tvNum = binding.tvNumeroMuestra
        tvRegM.text = tvFolio.text.toString() + "-" + tvNum.text.toString()
        val txtPrueba = findViewById<TextView>(R.id.txtnombre)
        val btnStart = findViewById<AppCompatButton>(R.id.btnStart)
        binding.btnStart.setOnClickListener {
            tvRegM.text = tvFolio.text.toString() + "-" + tvNum.text.toString()
            createMuestra()
            Toast.makeText(this, txtPrueba.text, Toast.LENGTH_SHORT).show()
            Log.i("Ray", "Boton Pulsado")
        }

        var myList = listOf("Example1", "Example2", "Example3")
        //val muestraUno = Muestra(1,32,"Agua de Red - NOM-000-123-345","20/05/2024 ")

        muestraProvider.listademuestras

        initRecyclerView()

    }

    private fun createMuestra() {
        val tvNum = binding.tvNumeroMuestra
        val tvfecham = binding.tvfechamuestreo
        val tvhoram = binding.tvHora
        val tvregistromuestra = binding.tvregistromuestra
        val txtnombrem = binding.txtnombre
        val txtcantidad = binding.txtcantidadaprox
        val txtTemp = binding.txtTemp
        val txtLugar = binding.txtLugar
        val txtDescripcion = binding.txtdescripcion
        val txtMicro = binding.txtMicro
        val txtFisico = binding.txtFisico
        val txtObserva = binding.txtobservaciones

        val numeroMuestra = tvNum.text

        if (numeroMuestra != null) {
            val fechaSinBarras = tvfecham.text.toString().replace("/", "")
            val horaSinPuntos = tvhoram.text.toString().replace(":", "")
            val horaRecortada = if (horaSinPuntos.length >= 4) horaSinPuntos.substring(0, 4) else horaSinPuntos
            val idLab = fechaSinBarras + horaRecortada

            // El valor de idServicio es un entero válido, puedes usarlo aquí
            val muestraobjeto =
                Muestra(
                    numeroMuestra = numeroMuestra.toString(),
                    fechaMuestra = tvfecham.text.toString(),
                    horaMuestra = tvhoram.text.toString(),
                    registroMuestra = tvregistromuestra.text.toString(),
                    nombreMuestra = txtnombrem.text.toString(),
                    idLab = idLab,
                    cantidadAprox = txtcantidad.text.toString(),
                    tempM = txtTemp.text.toString(),
                    lugarToma = txtLugar.text.toString(),
                    descripcionM = txtDescripcion.text.toString(),
                    emicro = txtMicro.text.toString(),
                    efisico = txtFisico.text.toString(),
                    observaciones = txtObserva.text.toString()
                )
            muestraMutableList.add(muestraobjeto)
            contador = muestraMutableList.size
            tvNum.text = (contador+1).toString()
            adapter.notifyItemInserted(muestraMutableList.size-1)
        } else {
            // Manejar el caso donde la conversión falló
            Toast.makeText(this, "Por favor, ingrese un número válido", Toast.LENGTH_SHORT).show()
            Log.i("Ray", "Ingrese numero valido")

        }




    }

    private fun initRecyclerView() {
        //val recyclerView = findViewById<RecyclerView>(R.id.recyclerMuestras)
        //adapter = muestraAdapter(){}
        adapter = muestraAdapter(
            muestraList = muestraMutableList,
            onClickListener = { muestra -> onItemSelected(muestra) },
            onclickDelete = { position -> onDeletedItem(position) })

        binding.recyclerMuestras.layoutManager = LinearLayoutManager(this)
        binding.recyclerMuestras.adapter = adapter

    }

    private fun onItemSelected(muestra: Muestra) {
        Toast.makeText(this, muestra.nombreMuestra, Toast.LENGTH_SHORT).show()
        Log.i("Ray", muestra.nombreMuestra)
    }

    private fun onDeletedItem(position: Int) {
        try {
            muestraMutableList.removeAt(position)
            //Notificar al listado que se ha en este caso borrado un item con una posicion
            adapter.notifyItemRemoved(position)
            val tvFolio = binding.tvFolio


            // Actualizar los números de muestra en la lista
            for (i in position until muestraMutableList.size) {
                muestraMutableList[i].numeroMuestra = (i + 1).toString()
                muestraMutableList[i].registroMuestra = tvFolio.text.toString()+ "-" +  muestraMutableList[i].numeroMuestra

            }
            adapter.notifyItemRangeChanged(position, muestraMutableList.size)

            // Actualizar contador y TextView de número de muestra
            contador = muestraMutableList.size
            binding.tvNumeroMuestra.text = (contador + 1).toString()
            Log.e("Prueba".toString(), "El contador es:$contador")

        }catch (e: Exception){
            Log.e("Error".toString(),"Hubo un error")
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable) // Detener la actualización de la hora cuando se destruye la actividad
    }


}