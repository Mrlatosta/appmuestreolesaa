package com.example.aplicacionlesaa


import RetrofitClient
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplicacionlesaa.adapter.muestraAdapter
import com.example.aplicacionlesaa.databinding.ActivityMainBinding
import com.example.aplicacionlesaa.model.ClientePdm
import com.example.aplicacionlesaa.model.Descripcion
import com.example.aplicacionlesaa.model.MuestraData
import com.example.aplicacionlesaa.model.Servicio
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    //private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    //Lista a la cual le vamos a quitar o poner muestras
    private val storagePermissionRequestCode = 1001
    private var muestraMutableList: MutableList<Muestra> =
        muestraProvider.listademuestras.toMutableList()
    private lateinit var adapter: muestraAdapter

    private val serviciosList: MutableList<Servicio> = mutableListOf()
    private val descripcionesList: MutableList<Descripcion> = mutableListOf()
    private var clientePdm: ClientePdm? = null
    private var pdmSeleccionado: String = ""
    private var folio: String? = null

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

        val serviciosRecibidos = intent.getParcelableArrayListExtra<Servicio>("listaServicios")
        if (serviciosRecibidos != null) {
            serviciosList.addAll(serviciosRecibidos)
        }

        clientePdm = intent.getParcelableExtra("clientePdm")

        println("El clientePdm es: $clientePdm")

        binding.tvCliente.text = clientePdm?.nombre_empresa

        //Inicio Api
        val apiService = RetrofitClient.instance
        val spinner: Spinner = binding.idSpinner1
        val txtDescripciones = binding.txtdescripcion
        folio = intent.getStringExtra("folio")
        binding.tvFolio.text = folio
        pdmSeleccionado = intent.getStringExtra("plandemuestreo") ?: "Error"
        println("El plan de muestreo es: " + pdmSeleccionado)
        binding.tvPDM.text = pdmSeleccionado

        apiService.getDescriptions().enqueue(object : Callback<List<Descripcion>> {
            override fun onResponse(
                call: Call<List<Descripcion>>,
                response: Response<List<Descripcion>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { descripciones ->

                        descripcionesList.addAll(descripciones)
                        println("La lista de descripciones es: " + descripcionesList)
                        val descris =
                            descripciones.map { it.descripcion.toString() } // Convertir IDs a Strings
                        // Configurar Autocompleteview
                        val adapter = ArrayAdapter(
                            this@MainActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            descris
                        )
                        txtDescripciones.setAdapter(adapter)
                    }
                } else {
                    Log.e("MainActivity", "Error en Autocomplete: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Descripcion>>, t: Throwable) {
                Log.e("MainActivity", "Failure en autocomplete: ${t.message}")
            }
        })



        println("La lista de servicios es: " + serviciosList)

        val ids = serviciosList.map { it.id.toString() } // Convertir IDs a Strings

        // Configurar el adaptador del Spinner con la lista de IDs

        val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, ids)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter


        val tvDescripcion = binding.tvdescripcionmuestra
        val tvCantidad = binding.tvCantidadRestante
        val txtCantidadAprox = binding.txtcantidadaprox
        val txtEmicro = binding.txtMicro
        val txtEfisico = binding.txtFisico

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Obtener el servicio seleccionado
                val servicioSeleccionado = serviciosList[position]
                println(position)
                try {
                    try {
                        txtEmicro.text.clear()
                    } catch (e: Exception) {
                        Log.e("Error", "Error al limpiar txtEmicro")
                    }

                    try {
                        txtEfisico.text.clear()
                    } catch (e: Exception) {
                        Log.e("Error", "Error al limpiar txtEfisico")
                    }

                    try {
                        txtCantidadAprox.text.clear()
                    } catch (e: Exception) {
                        Log.e("Error", "Error al limpiar txtCantidadAprox")
                    }

                    try {
                        tvDescripcion.text = servicioSeleccionado.descripcion
                    } catch (e: Exception) {
                        Log.e("Error", "Error al establecer la descripción en tvDescripcion")
                    }

                    try {
                        tvCantidad.text = servicioSeleccionado.cantidad.toString()
                    } catch (e: Exception) {
                        Log.e("Error", "Error al establecer la cantidad en tvCantidad")
                    }

                    try {
                        txtEmicro.text = Editable.Factory.getInstance()
                            .newEditable(servicioSeleccionado.estudios_microbiologicos)
                    } catch (e: Exception) {
                        Log.e(
                            "Error",
                            "Error al establecer los estudios microbiológicos en txtEmicro"
                        )
                    }

                    try {
                        txtEfisico.text = Editable.Factory.getInstance()
                            .newEditable(servicioSeleccionado.estudios_fisicoquimicos)
                    } catch (e: Exception) {
                        Log.e(
                            "Error",
                            "Error al establecer los estudios físicoquímicos en txtEfisico"
                        )
                    }

                    try {
                        txtCantidadAprox.text = Editable.Factory.getInstance()
                            .newEditable(servicioSeleccionado.cantidad_de_toma)
                    } catch (e: Exception) {
                        Log.e(
                            "Error",
                            "Error al establecer la cantidad aproximada en txtCantidadAprox"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("Error", "Error al mostrar los datos en los txt y tv")
                }

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
        //val tvhoram = binding.tvHora
        val tvFolio = binding.tvFolio

        //val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE


        //ActivityCompat.requestPermissions(this, arrayOf(permission), "1003")


        try {
            println("Hola")
            //obtenerServicios()
        } catch (e: Exception) {
            Log.e("Error", "Error al obtener los servicios")
        }

        val btnSiguiente = binding.btnSiguiente

        btnSiguiente.setOnClickListener {

            showConfirmationDialog()

            //checkStoragePermissionAndSavePdf()
        }

        // Establecer la fecha actual
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        tvfecham.text = currentDate

        /*// Configurar el handler para actualizar la hora
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
        handler.post(runnable)*/


        val tvRegM = binding.tvregistromuestra
        val tvNum = binding.tvNumeroMuestra
        tvRegM.text = tvFolio.text.toString() + "-" + tvNum.text.toString()
        var sepudo = false
        binding.btnStart.setOnClickListener {

            sepudo = createMuestra()
            if (sepudo == true) {
                tvRegM.text = tvFolio.text.toString() + "-" + tvNum.text.toString()
                clearTextFields()
                Log.i("Ray", "Boton Pulsado")
            }
            checkStoragePermissionAndSavePdf()


        }

        //val muestraUno = Muestra(1,32,"Agua de Red - NOM-000-123-345","20/05/2024 ")

        muestraProvider.listademuestras

        initRecyclerView()


    }

    override fun onBackPressed() {
        // Crear un AlertDialog para la confirmación
        AlertDialog.Builder(this).apply {
            setTitle("Confirmación")
            setMessage("¿Estás seguro de que deseas salir?")
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
    }

    private fun showConfirmationDialog() {
        val totalServiciosFaltante = serviciosList.filter { it.cantidad > 0 }.sumOf { it.cantidad }
        // Crear y mostrar el cuadro de diálogo de confirmación
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmación")
        if (totalServiciosFaltante > 0) {
            builder.setMessage("¿Estás seguro de que quieres avanzar, quedan $totalServiciosFaltante servicios sin asignar?")
        } else {
            builder.setMessage("¿Estás seguro de que quieres avanzar?")
        }
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
        Log.e("Hola", "Hola boton siguiente apretado")
        val intent = Intent(this, MainActivity2::class.java)
        intent.putParcelableArrayListExtra("muestraList", ArrayList(muestraMutableList))
        intent.putExtra("plandemuestreo", pdmSeleccionado)
        intent.putExtra("clientePdm", clientePdm)
        intent.putExtra("folio", folio)




        startActivity(intent)
    }


    private fun clearTextFields() {

        val txtnombrem = binding.txtnombre
        val txtTemp = binding.txtTemp
        val txtLugar = binding.txtLugar
        val txtDescripcion = binding.txtdescripcion
        val txtObserva = binding.txtobservaciones

        txtnombrem.text.clear()
        txtTemp.text.clear()
        txtLugar.text.clear()
        txtDescripcion.text.clear()
        txtObserva.text.clear()

    }

    private fun createMuestra(): Boolean {
        var sepudo = false
        val tvNum = binding.tvNumeroMuestra
        val tvfecham = binding.tvfechamuestreo
        //val tvhoram = binding.tvHora
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
        var a = true

        if (txtnombrem.text.toString().trim().isEmpty() || txtcantidad.text.toString().trim()
                .isEmpty() || txtTemp.text.toString().trim().isEmpty() || txtLugar.text.toString()
                .trim().isEmpty() || txtDescripcion.text.toString().trim().isEmpty()
        )        {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            sepudo = false
        } else {
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
                println(servicioSeleccionado.id.toString() + "= " + spinner1.selectedItem.toString())
                if (servicioSeleccionado.id == spinner1.selectedItem.toString().toInt()) {
                    tvCantidad.text = servicioSeleccionado.cantidad.toString()
                }


                val numeroMuestra = tvNum.text

                if (numeroMuestra != null) {
                    val formatoEntrada = SimpleDateFormat("MM/dd/yyyy")
                    val formatoSalida = SimpleDateFormat("yyyyMMdd")

                    val fecha = formatoEntrada.parse(tvfecham.text.toString())

                    // Formatea la fecha al nuevo formato sin barras
                    val fechaSinBarras = formatoSalida.format(fecha)


                    //val fechaSinBarras = tvfecham.text.toString().replace("/", "")
                    //val horaSinPuntos = tvhoram.text.toString().replace(":", "")
                    /*val horaRecortada =
                        if (horaSinPuntos.length >= 4) horaSinPuntos.substring(
                            0,
                            4
                        ) else horaSinPuntos*/
                    val idLab = fechaSinBarras + tvregistromuestra.text.toString()

                    // El valor de idServicio es un entero válido, puedes usarlo aquí
                    val muestraobjeto =
                        Muestra(
                            numeroMuestra = numeroMuestra.toString(),
                            fechaMuestra = tvfecham.text.toString(),
                            horaMuestra = "",
                            registroMuestra = tvregistromuestra.text.toString(),
                            nombreMuestra = txtnombrem.text.toString().trim(),
                            idLab = idLab,
                            cantidadAprox = txtcantidad.text.toString().trim(),
                            tempM = txtTemp.text.toString().trim(),
                            lugarToma = txtLugar.text.toString().trim(),
                            descripcionM = txtDescripcion.text.toString().trim(),
                            emicro = txtMicro.text.toString().trim(),
                            efisico = txtFisico.text.toString().trim(),
                            observaciones = txtObserva.text.toString().trim(),
                            servicioId = idServicioEntero
                        )
                    muestraMutableList.add(muestraobjeto)
                    contador = muestraMutableList.size
                    tvNum.text = (contador + 1).toString()

                    adapter.notifyItemInserted(muestraMutableList.size - 1)
                    Toast.makeText(this, "Se ha añadido la muestra", Toast.LENGTH_SHORT).show()
                    sepudo = true
                } else {
                    // Manejar el caso donde la conversión falló
                    Toast.makeText(this, "Por favor, ingrese un número válido", Toast.LENGTH_SHORT)
                        .show()
                    Log.i("Ray", "Ingrese numero valido")

                }

            } else {
                Toast.makeText(
                    this,
                    "No hay suficiente cantidad disponible para este servicio",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

        //Toast.makeText(this, "El estado de sepudo es: $sepudo", Toast.LENGTH_SHORT).show()
        return sepudo
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
                println(muestraEliminada.servicioId.toString() + "= " + spinner1.selectedItem.toString())
                if (muestraEliminada.servicioId == spinner1.selectedItem.toString().toInt()) {
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
            binding.tvregistromuestra.text =
                tvFolio.text.toString() + "-" + binding.tvNumeroMuestra.text.toString()
            Log.e("Prueba".toString(), "El contador es:$contador")

            checkStoragePermissionAndSavePdf()


        } catch (e: Exception) {
            Log.e("Error".toString(), "Hubo un error")
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        //.removeCallbacks(runnable) // Detener la actualización de la hora cuando se destruye la actividad
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
                val arrayAdapter =
                    ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, ids)
                spinner.adapter = arrayAdapter
            }
        }
    }

    private fun checkStoragePermissionAndSavePdf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + applicationContext.packageName)
                startActivityForResult(intent, storagePermissionRequestCode)
            } else {
                val muestraData = MuestraData(
                    binding.tvFolio.text.toString(),
                    pdmSeleccionado,
                    muestraMutableList
                )
                saveDataToJson(this, muestraData, "Datos-folio-${binding.tvFolio.text}.json")
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    storagePermissionRequestCode
                )
            } else {
                val muestraData = MuestraData(
                    binding.tvFolio.text.toString(),
                    pdmSeleccionado,
                    muestraMutableList
                )
                saveDataToJson(this, muestraData, "Datos-folio-${binding.tvFolio.text}.json")
            }
        }
    }

    fun saveDataToJson(context: Context, muestraData: MuestraData, filename: String) {
        val gson = Gson()
        val jsonString = gson.toJson(muestraData)

        // Obtener la ruta de la carpeta Documents
        val documentsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .toString()


        // Crear el archivo en la carpeta Documents
        val file = File(documentsDir, filename)

        // Escribir el archivo
        file.writeText(jsonString)
    }
}