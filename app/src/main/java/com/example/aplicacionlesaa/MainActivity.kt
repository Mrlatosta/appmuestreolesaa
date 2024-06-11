package com.example.aplicacionlesaa


import RetrofitClient
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
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
import com.example.aplicacionlesaa.model.Servicio
import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    //Lista a la cual le vamos a quitar o poner muestras
    private val storagePermissionRequestCode = 1001
    private var muestraMutableList: MutableList<Muestra> =
        muestraProvider.listademuestras.toMutableList()
    private lateinit var adapter: muestraAdapter

    private val serviciosList: MutableList<Servicio> = mutableListOf()

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

        //Inicio Api
        val apiService = RetrofitClient.instance
        val spinner: Spinner = findViewById(R.id.idSpinner1)


        apiService.getPlanServicesByName("pmd-1").enqueue(object : Callback<List<Servicio>> {
            override fun onResponse(call: Call<List<Servicio>>, response: Response<List<Servicio>>) {
                if (response.isSuccessful) {
                    response.body()?.let { servicios ->
                        for (servicio in servicios) {
                            serviciosList.addAll(servicios)
//                            Log.d("MainActivity", "ID: ${servicio.id}, Cantidad: ${servicio.cantidad}, " +
//                                    "Estudios Micro: ${servicio.estudios_microbiologicos}, Estudios Fisico: ${servicio.estudios_fisicoquimicos}, " +
//                                    "Descripcion: ${servicio.descripcion}, Cantidad Toma: ${servicio.cantidad_de_toma}")
                            println("La lista de servicios es: "+ serviciosList)
                            val ids = servicios.map { it.id.toString() } // Convertir IDs a Strings

                            // Configurar el adaptador del Spinner con la lista de IDs

                            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, ids)
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinner.adapter = adapter


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

        val tvDescripcion = binding.tvdescripcionmuestra
        val tvCantidad = binding.tvCantidadRestante
        val txtCantidadAprox = binding.txtcantidadaprox
        val txtEmicro = binding.txtMicro
        val txtEfisico = binding.txtFisico

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Obtener el servicio seleccionado
                val servicioSeleccionado = serviciosList[position]
                println(position)

                // Actualizar el TextView con la descripción del servicio seleccionado
                tvDescripcion.text = servicioSeleccionado.descripcion
                tvCantidad.text = servicioSeleccionado.cantidad.toString()
                txtCantidadAprox.text = Editable.Factory.getInstance().newEditable(servicioSeleccionado.cantidad_de_toma)
                txtEmicro.text = Editable.Factory.getInstance().newEditable(servicioSeleccionado.estudios_microbiologicos)
                txtEfisico.text = Editable.Factory.getInstance().newEditable(servicioSeleccionado.estudios_fisicoquimicos)

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Manejar la situación en la que no se ha seleccionado nada en el Spinner (opcional)
            }
        }


        //obtenerServicios()
        //val spinner: Spinner = findViewById(R.id.idSpinner1)
        //val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, obtenerServicios())
        //spinner.adapter = arrayAdapter

        val tvfecham = binding.tvfechamuestreo
        val tvhoram = binding.tvHora
        val tvFolio = binding.tvFolio

        //val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE


        //ActivityCompat.requestPermissions(this, arrayOf(permission), "1003")


        try{
            println("Hola")
            //obtenerServicios()
            }catch (e:Exception){
                Log.e("Error", "Error al obtener los servicios")
            }

        val btnSiguiente = binding.btnSiguiente

        btnSiguiente.setOnClickListener {
            Log.e("Hola", "Hola boton siguiente apretado")
            val intent = Intent(this, MainActivity2::class.java)
            intent.putParcelableArrayListExtra("muestraList", ArrayList(muestraMutableList))
            startActivity(intent)

            //checkStoragePermissionAndSavePdf()
        }

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
            createMuestra()
            tvRegM.text = tvFolio.text.toString() + "-" + tvNum.text.toString()
            clearTextFields()
            Toast.makeText(this, txtPrueba.text, Toast.LENGTH_SHORT).show()
            Log.i("Ray", "Boton Pulsado")

        }

        //val muestraUno = Muestra(1,32,"Agua de Red - NOM-000-123-345","20/05/2024 ")

        muestraProvider.listademuestras

        initRecyclerView()

    }

    private fun clearTextFields(){

        val txtnombrem = binding.txtnombre
        val txtcantidad = binding.txtcantidadaprox
        val txtTemp = binding.txtTemp
        val txtLugar = binding.txtLugar
        val txtDescripcion = binding.txtdescripcion
        val txtMicro = binding.txtMicro
        val txtFisico = binding.txtFisico
        val txtObserva = binding.txtobservaciones

        txtnombrem.text.clear()
        txtcantidad.text.clear()
        txtTemp.text.clear()
        txtLugar.text.clear()
        txtDescripcion.text.clear()
        txtMicro.text.clear()
        txtFisico.text.clear()
        txtObserva.text.clear()

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
        val txtServicioId = binding.idSpinner1
        val idServicioString = txtServicioId.selectedItem.toString()
        var idServicioEntero: Int = 0
        var tvCantidad = binding.tvCantidadRestante
        val spinner1 = binding.idSpinner1

        try {
            idServicioEntero = idServicioString.toInt()
        } catch (e: NumberFormatException) {
            // Manejar la situación en la que la cadena no puede ser convertida a un entero
            // Aquí puedes mostrar un mensaje de error o tomar alguna acción alternativa
        }

        val servicioSeleccionado = serviciosList.find { it.id == idServicioEntero }

        if (servicioSeleccionado != null && servicioSeleccionado.cantidad > 0) {
            // Restar la cantidad al servicio
            servicioSeleccionado.cantidad--
            println(    servicioSeleccionado.id.toString() + "= " + spinner1.selectedItem.toString() )
            if (servicioSeleccionado.id == spinner1.selectedItem.toString().toInt() ){
                tvCantidad.text = servicioSeleccionado.cantidad.toString()
            }







            val numeroMuestra = tvNum.text

        if (numeroMuestra != null) {
            val fechaSinBarras = tvfecham.text.toString().replace("/", "")
            val horaSinPuntos = tvhoram.text.toString().replace(":", "")
            val horaRecortada =
                if (horaSinPuntos.length >= 4) horaSinPuntos.substring(0, 4) else horaSinPuntos
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
                    observaciones = txtObserva.text.toString(),
                    servicioId = idServicioEntero
                )
            muestraMutableList.add(muestraobjeto)
            contador = muestraMutableList.size
            tvNum.text = (contador + 1).toString()

            adapter.notifyItemInserted(muestraMutableList.size - 1)
        } else {
            // Manejar el caso donde la conversión falló
            Toast.makeText(this, "Por favor, ingrese un número válido", Toast.LENGTH_SHORT).show()
            Log.i("Ray", "Ingrese numero valido")

        }

        }else{
            Toast.makeText(this, "No hay suficiente cantidad disponible para este servicio", Toast.LENGTH_SHORT).show()
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
            val muestraEliminada = muestraMutableList[position]
            val tvCantidad = binding.tvCantidadRestante
            val servicioAsociado = serviciosList.find { it.id == muestraEliminada.servicioId }
            val spinner1 = binding.idSpinner1

            // Verificar si se encontró el servicio asociado
            if (servicioAsociado != null) {
                // Incrementar la cantidad del servicio
                servicioAsociado.cantidad++
                println(    muestraEliminada.servicioId.toString() + "= " + spinner1.selectedItem.toString() )
                if (muestraEliminada.servicioId == spinner1.selectedItem.toString().toInt() ){
                    tvCantidad.text = servicioAsociado.cantidad.toString()
                }

            }

            muestraMutableList.removeAt(position)
            //Notificar al listado que se ha en este caso borrado un item con una posicion
            adapter.notifyItemRemoved(position)
            val tvFolio = binding.tvFolio


            // Actualizar los números de muestra en la lista
            for (i in position until muestraMutableList.size) {
                muestraMutableList[i].numeroMuestra = (i + 1).toString()
                muestraMutableList[i].registroMuestra =
                    tvFolio.text.toString() + "-" + muestraMutableList[i].numeroMuestra

            }
            adapter.notifyItemRangeChanged(position, muestraMutableList.size)

            // Actualizar contador y TextView de número de muestra
            contador = muestraMutableList.size
            binding.tvNumeroMuestra.text = (contador + 1).toString()
            binding.tvregistromuestra.text = tvFolio.text.toString() + "-" + binding.tvNumeroMuestra.text.toString()
            Log.e("Prueba".toString(), "El contador es:$contador")





        } catch (e: Exception) {
            Log.e("Error".toString(), "Hubo un error")
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable) // Detener la actualización de la hora cuando se destruye la actividad
    }


    private fun obtenerServicios() {
        AsyncTask.execute {
            val ids = mutableListOf<String>()
            val connection = DriverManager.getConnection(
                "jdbc:postgresql://db-grupolesaa-rds.c1qss02m236z.us-east-1.rds.amazonaws.com:5432/dbgrupolesaa",
                "postgres",
                "Lara1234"
            )
            val query = """
            SELECT servicios.id
            FROM servicios
            JOIN estudios ON estudios.clave_interna = servicios.estudio_clave_interna
            WHERE servicios.folio_id = ?
        """.trimIndent()
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setString(1, "FCLHTL-LAB-062-COT-001")

            val resultSet = preparedStatement.executeQuery()
            while (resultSet.next()) {
                val id = resultSet.getString("id")
                ids.add(id)
            }
            resultSet.close()
            preparedStatement.close()
            connection.close()

            // Actualizar el spinner en el hilo principal
            runOnUiThread {
                val spinner: Spinner = findViewById(R.id.idSpinner1)
                val arrayAdapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, ids)
                spinner.adapter = arrayAdapter
            }
        }
    }
}