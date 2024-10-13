package com.example.aplicacionlesaa


import DragManageAdapter
import RetrofitClient
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.adapter.ServicioAdapterInfo
import com.example.aplicacionlesaa.adapter.muestraAdapter
import com.example.aplicacionlesaa.databinding.ActivityMainBinding
import com.example.aplicacionlesaa.model.ClientePdm
import com.example.aplicacionlesaa.model.Descripcion
import com.example.aplicacionlesaa.model.MuestraData
import com.example.aplicacionlesaa.model.Pdm
import com.example.aplicacionlesaa.model.Servicio
import com.example.aplicacionlesaa.utils.OnItemMovedListener
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity(), OnItemMovedListener {

    private lateinit var binding: ActivityMainBinding

    //private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var modoEdicion = false

    //Lista a la cual le vamos a quitar o poner muestras
    private val storagePermissionRequestCode = 1001
    private var muestraMutableList: MutableList<Muestra> =
        muestraProvider.listademuestras.toMutableList()
    private lateinit var adapter: muestraAdapter
    private var indexMuestraAEditar: Int = -1
    private lateinit var pdmDetallado: Pdm
    private val serviciosList: MutableList<Servicio> = mutableListOf()
    private var descripcionesList: MutableList<Descripcion> = mutableListOf()
    private var clientePdm: ClientePdm? = null
    private var pdmSeleccionado: String = ""
    private var folio: String? = null
    private var lugares: ArrayList<String> = ArrayList()
    private var adapterEdicion: muestraAdapter? = null
    private var fechaSinBarras: String = ""
    private var existeExtra: Boolean = false
    private var muestrasExtras: ArrayList<Muestra> = ArrayList()
    val subtipos = mutableListOf<String>()




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

        lugares = intent.getStringArrayListExtra("lugares") ?: ArrayList()
        println("La lista de lugares es: $lugares")
        for(lugar in lugares){
            println("El lugar es: $lugar")
        }



        val adapterLugares =
            ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, lugares)

        binding.txtLugar.setAdapter(adapterLugares)


        var descripcionesLista = intent.getParcelableArrayListExtra<Descripcion>("descripciones")
        if (descripcionesLista != null) {
            descripcionesList.addAll(descripcionesLista)
            println("La lista de descripciones es: $descripcionesList")
        }

        val descris =
            descripcionesList.map { it.descripcion.toString() } // Convertir IDs a Strings
        // Configurar Autocompleteview
        val adapterDesci = ArrayAdapter(
            this@MainActivity,
            android.R.layout.simple_spinner_dropdown_item,
            descris
        )

        binding.txtdescripcion.setAdapter(adapterDesci)

        binding.txtLugar.setOnClickListener(View.OnClickListener {
            binding.txtLugar.showDropDown()
        })

        binding.txtdescripcion.setOnClickListener(View.OnClickListener {
            binding.txtdescripcion.showDropDown()
        })

        pdmDetallado = intent.getParcelableExtra("pdmDetallado")!!
        Log.e("Thayli", "El pdmDetallado es: $pdmDetallado")
        //Inicio Api
        val apiService = RetrofitClient.instance
        val spinner: Spinner = binding.idSpinner1
        val spinnerSubtipo: Spinner = binding.idspinnerSubtipo
        val txtDescripciones = binding.txtdescripcion
        folio = intent.getStringExtra("folio")
        binding.tvFolio.text = folio
        pdmSeleccionado = intent.getStringExtra("plandemuestreo") ?: "Error"
        println("El plan de muestreo es: " + pdmSeleccionado)
        binding.tvPDM.text = pdmSeleccionado

        println("La lista de servicios es: " + serviciosList)

        binding.btnInfo.setOnClickListener {
            showServicioDialog()
        }

        val ids = serviciosList.map { it.id.toString() } // Convertir IDs a Strings

        // Configurar el adaptador del Spinner con la lista de IDs

        val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, ids)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        //Create an empty array of strings




        val adapterSubtipo = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, subtipos)
        adapterSubtipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSubtipo.adapter = adapterSubtipo


        val tvDescripcion = binding.tvdescripcionmuestra
        val tvCantidad = binding.tvCantidadRestante
        val txtCantidadAprox = binding.txtcantidadaprox
        val txtEmicro = binding.txtMicro
        val txtEfisico = binding.txtFisico
        val txtNombre = binding.txtnombre

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
                    try{
                        if (servicioSeleccionado.cantidad == 0){
                            tvCantidad.setTextColor(resources.getColor(R.color.red))
                        }else{
                            tvCantidad.setTextColor(resources.getColor(R.color.green))
                        }
                        }catch (e:Exception) {
                        Log.e("Error", "Error al establecer color en tvCantidad")
                    }

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
                        txtNombre.text.clear()
                    } catch (e: Exception) {
                        Log.e("Error", "Error al limpiar txtNombre")
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

                    try{
                        if (servicioSeleccionado.descripcion.contains("Agua de alberca") ||
                            servicioSeleccionado.descripcion.contains("Agua de Alberca") ||
                            servicioSeleccionado.descripcion.contains("AGUA DE ALBERCA") ) {
                            txtNombre.text = Editable.Factory.getInstance().newEditable("Agua de Alberca")
                        }
                    }catch (e:Exception){
                        Log.e("Error", "Error al establecer el nombre en txtNombre")
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

                    if (servicioSeleccionado.clasificacion == "ALIMENTOS COCIDOS"){
                        //Eliminar si hay contenido en subtipos
                        subtipos.clear()
                        //Add to subtipos array the string: hola
                        subtipos.add("Cocidos")
                        subtipos.add("Salsas y pures cocidos")
                        subtipos.add("Ensaladas cocidas")
                    }else if (servicioSeleccionado.clasificacion == "ALIMENTOS CRUDOS LISTO PARA CONSUMO  (ENSALADAS VERDES, CRUDAS O DE FRUTAS )"){
                        subtipos.clear()
                        subtipos.add("Crudos listos para consumo")
                        subtipos.add("Pulpas")
                        subtipos.add("JUGOS")
                        subtipos.add("AGUAS PREPARADAS")
                        subtipos.add("Carnicos no listos para el consumo")
                        subtipos.add("Carnicos crudos listos para consumo")
                        subtipos.add("Productos de la pesca crudos")
                        subtipos.add("Ahumados")
                    }else if (servicioSeleccionado.clasificacion == "POSTRES"){
                        subtipos.clear()
                        subtipos.add("Postres lacteos")
                        subtipos.add("Postres a base de harina")
                        subtipos.add("Postres no lacteos")
                        subtipos.add("Helados")

                    }else{
                        subtipos.clear()
                        subtipos.add("")
                    }

                    adapterSubtipo.notifyDataSetChanged()

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
            if (modoEdicion == true) {
                Toast.makeText(this, "No se puede avanzar en modo edicion", Toast.LENGTH_SHORT).show()
            }else{
                showConfirmationDialog()
            }

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

            if (modoEdicion == true){
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Confirmación")
                builder.setMessage("¿Estás seguro de que deseas editar la muestra?")
                builder.setPositiveButton("Sí") { dialog, which ->
                    try {


                        if (binding.txtnombre.text.trim().isEmpty() ||
                            binding.txtcantidadaprox.text.trim().isEmpty() ||
                            binding.txtTemp.text.trim().isEmpty() ||
                            binding.txtLugar.text.trim().isEmpty() ||
                            binding.txtdescripcion.text.trim().isEmpty()
                        ) {

                            Toast.makeText(
                                this,
                                "Por favor, complete todos los campos",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            //Tomar el servicio id para ver su clasificacion en base a su index
                            var servicioId = muestraMutableList[indexMuestraAEditar].servicioId
                            var servicio = serviciosList.find { it.id == servicioId }
                            if (servicio != null) {
                                var clasificacion = servicio.clasificacion
                                if (clasificacion == "ALIMENTOS COCIDOS"){
                                    //Eliminar si hay contenido en subtipos
                                    subtipos.clear()
                                    //Add to subtipos array the string: hola
                                    subtipos.add("Cocidos")
                                    subtipos.add("Salsas y pures cocidos")
                                    subtipos.add("Ensaladas cocidas")
                                }else if (clasificacion == "ALIMENTOS CRUDOS LISTO PARA CONSUMO  (ENSALADAS VERDES, CRUDAS O DE FRUTAS )"){
                                    subtipos.clear()
                                    subtipos.add("Crudos listos para consumo")
                                    subtipos.add("Pulpas")
                                    subtipos.add("JUGOS")
                                    subtipos.add("AGUAS PREPARADAS")
                                    subtipos.add("Carnicos no listos para el consumo")
                                    subtipos.add("Carnicos crudos listos para consumo")
                                    subtipos.add("Productos de la pesca crudos")
                                    subtipos.add("Ahumados")
                                }else if (clasificacion == "POSTRES"){
                                    subtipos.clear()
                                    subtipos.add("Postres lacteos")
                                    subtipos.add("Postres a base de harina")
                                    subtipos.add("Postres no lacteos")
                                    subtipos.add("Helados")

                                }else{
                                    subtipos.clear()
                                    subtipos.add("")
                                }
                                adapterSubtipo.notifyDataSetChanged()
                            }



                            muestraMutableList[indexMuestraAEditar].nombreMuestra =
                                binding.txtnombre.text.toString()
                            muestraMutableList[indexMuestraAEditar].cantidadAprox =
                                binding.txtcantidadaprox.text.toString()
                            muestraMutableList[indexMuestraAEditar].tempM =
                                binding.txtTemp.text.toString()
                            muestraMutableList[indexMuestraAEditar].lugarToma =
                                binding.txtLugar.text.toString()
                            muestraMutableList[indexMuestraAEditar].descripcionM =
                                binding.txtdescripcion.text.toString()
                            muestraMutableList[indexMuestraAEditar].emicro =
                                binding.txtMicro.text.toString()
                            muestraMutableList[indexMuestraAEditar].efisico =
                                binding.txtFisico.text.toString()
                            muestraMutableList[indexMuestraAEditar].observaciones =
                                binding.txtobservaciones.text.toString()

                            muestraMutableList[indexMuestraAEditar].subtipo =
                                binding.idspinnerSubtipo.selectedItem.toString()

                            adapterEdicion?.notifyItemChanged(indexMuestraAEditar)
                            clearTextFields()
                            Toast.makeText(this, "Muestra editada", Toast.LENGTH_SHORT).show()
                            setEditMode(false)
                            binding.tvTitulo.text = "Registro de Muestras"
                            binding.btnStart.text = "Agregar"
                            indexMuestraAEditar = -1
                            binding.idSpinner1.isEnabled = true

                        }

                    } catch (e: Exception) {
                        Log.e("Error".toString(), "Hubo un error ${e}")
                        setEditMode(false)
                        binding.tvTitulo.text = "Registro de Muestras"
                        binding.btnStart.text = "Agregar"
                        binding.idSpinner1.isEnabled = true
                        clearTextFields()
                        Toast.makeText(
                            this,
                            "Error al editar la muestra, Saliendo del modo edicion",
                            Toast.LENGTH_SHORT
                        ).show()
                        indexMuestraAEditar = -1
                    }

                }

                builder.setNegativeButton("No") { dialog, which ->
                    dialog.dismiss()
                }

                builder.setNeutralButton("Cancelar Edicion") { dialog, which ->
                    setEditMode(false)
                    binding.tvTitulo.text = "Registro de Muestras"
                    binding.btnStart.text = "Agregar"
                    clearTextFields()
                    Toast.makeText(
                        this,
                        "Saliendo del modo edicion",
                        Toast.LENGTH_SHORT
                    ).show()
                    indexMuestraAEditar = -1
                }

                builder.show()





            }else{
                //Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                sepudo = createMuestra()
                if (sepudo == true) {
                    tvRegM.text = tvFolio.text.toString() + "-" + tvNum.text.toString()
                    clearTextFields()
                    Log.i("Ray", "Boton Pulsado")
                }
                checkStoragePermissionAndSaveJson()
            }



        }



        binding.btnMuestraExtra.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmación")
            builder.setMessage("¿Estás seguro de que deseas agregar una muestra extra?")
            builder.setPositiveButton("Sí") { dialog, which ->
                try {
                    val intent = Intent(this, MuestraExtraActivity::class.java)
                    intent.putExtra("folio", folio)
                    intent.putExtra("pdmDetallado", pdmDetallado)
                    intent.putExtra("clientePdm", clientePdm)
                    intent.putStringArrayListExtra("lugares", lugares)
                    intent.putExtra("descripciones", descripcionesList as ArrayList<Descripcion>)

                    if (muestrasExtras.isNotEmpty()) {
                        intent.putParcelableArrayListExtra(
                            "muestraList",
                            ArrayList(muestrasExtras)
                        )
                        Log.e("muestras:", muestrasExtras.toString())
                        Toast.makeText(this, "Ya hay muestras extras", Toast.LENGTH_SHORT).show()
                    }else{
                        intent.putParcelableArrayListExtra("muestraList", ArrayList())
                        Toast.makeText(this, "No hay muestras extras", Toast.LENGTH_SHORT).show()
                    }

                    startActivityForResult(intent, REQUEST_AGREGAR_MUESTRA)


                }catch (e:Exception){
                    Log.e("Error".toString(), "Hubo un error ${e}")
                }

                }
            builder.setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }
            builder.show()

        }

        //val muestraUno = Muestra(1,32,"Agua de Red - NOM-000-123-345","20/05/2024 ")

        muestraProvider.listademuestras

        initRecyclerView()


    }

    companion object {
        const val REQUEST_AGREGAR_MUESTRA = 1002

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_AGREGAR_MUESTRA && resultCode == Activity.RESULT_OK) {


            muestrasExtras= data?.getParcelableArrayListExtra("muestrasList")!!
            muestrasExtras?.let { list ->
                for (muestra in list) {
                    Log.e("Muestra", muestra.toString())
                }
                binding.tvMuestaEta.text = "Muestras Extra: " + list.size.toString()

            } ?: run {
                Log.e("Error", "No se recibieron muestras")
            }
        } else {
            Log.e("Error", "Error al obtener la muestra")
            }
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
        intent.putExtra("pdmDetallado", pdmDetallado)
        intent.putParcelableArrayListExtra("listaServicios", ArrayList(serviciosList))
        if (muestrasExtras.isNotEmpty()) {
            intent.putParcelableArrayListExtra("muestraExtraList", ArrayList(muestrasExtras))
            Log.e("muestras:", muestrasExtras.toString())
        }

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
        var idServicioEntero: String = String()
        var tvCantidad = binding.tvCantidadRestante
        val spinner1 = binding.idSpinner1
        val subtipo = binding.idspinnerSubtipo

        if (txtnombrem.text.toString().trim().isEmpty() || txtcantidad.text.toString().trim()
                .isEmpty() || txtTemp.text.toString().trim().isEmpty() || txtLugar.text.toString()
                .trim().isEmpty() || txtDescripcion.text.toString().trim().isEmpty()
        )
//        var f=false
//        if (f==true)
        {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            sepudo = false
        } else {
            try {
                idServicioEntero = idServicioString
            } catch (e: NumberFormatException) {
                // Manejar la situación en la que la cadena no puede ser convertida a un entero
                // Aquí puedes mostrar un mensaje de error o tomar alguna acción alternativa
            }

            val servicioSeleccionado = serviciosList.find { it.id == idServicioEntero }

            if (servicioSeleccionado != null && servicioSeleccionado.cantidad > 0) {
                // Restar la cantidad al servicio
                servicioSeleccionado.cantidad--
                println(servicioSeleccionado.id.toString() + "= " + spinner1.selectedItem.toString())
                if (servicioSeleccionado.id == spinner1.selectedItem.toString()) {
                    tvCantidad.text = servicioSeleccionado.cantidad.toString()
                    if (servicioSeleccionado.cantidad == 0) {
                        tvCantidad.setTextColor(resources.getColor(R.color.red))
                    }else{
                        tvCantidad.setTextColor(resources.getColor(R.color.green))
                    }
                }


                val numeroMuestra = tvNum.text

                if (numeroMuestra != null) {
                    val formatoEntrada = SimpleDateFormat("MM/dd/yyyy")
                    val formatoSalida = SimpleDateFormat("yyyyMMdd")

                    val fecha = formatoEntrada.parse(tvfecham.text.toString())

                    // Formatea la fecha al nuevo formato sin barras
                     fechaSinBarras = formatoSalida.format(fecha)


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
                            registroMuestra = tvregistromuestra.text.toString(),
                            nombreMuestra = txtnombrem.text.toString().trim(),
                            idLab = idLab,
                            cantidadAprox = txtcantidad.text.toString().trim(),
                            tempM = txtTemp.text.toString().trim()+"°C",
                            lugarToma = txtLugar.text.toString().trim(),
                            descripcionM = txtDescripcion.text.toString().trim(),
                            emicro = txtMicro.text.toString().trim(),
                            efisico = txtFisico.text.toString().trim(),
                            observaciones = txtObserva.text.toString().trim(),
                            servicioId = idServicioEntero,
                            subtipo = subtipo.selectedItem.toString()
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
            onclickDelete = { position -> onDeletedItem(position) },
            onclickEdit = { position -> onEditItem(position) },
            this)

        binding.recyclerMuestras.layoutManager = LinearLayoutManager(this)
        binding.recyclerMuestras.adapter = adapter
        val callback = DragManageAdapter(adapter) {modoEdicion}
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.recyclerMuestras)


    }
    override fun onItemMoved() {
        // Implementa la lógica para guardar los datos cuando un item se mueva
        checkStoragePermissionAndSaveJson()
        Log.i("Ray", "Se ha movido un item")
    }


    private fun onItemSelected(muestra: Muestra) { //pendiente
        if (modoEdicion == true){
            Toast.makeText(this, "No se puede copiar una muestra en modo edicion", Toast.LENGTH_SHORT).show()
        }else{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmación")
            builder.setMessage("¿Estás seguro de que deseas copiar la muestra ${muestra.nombreMuestra}?")
            builder.setPositiveButton("Sí") { dialog, which ->
                try {
                    clearTextFields()
                    binding.txtnombre.setText(muestra.nombreMuestra)
                    binding.txtTemp.setText(muestra.tempM)
                    binding.txtLugar.setText(muestra.lugarToma)
                    binding.txtdescripcion.setText(muestra.descripcionM)
                } catch (e: Exception) {
                    Log.e("Error".toString(), "Hubo un error ${e}")
                }

            }

            builder.setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }

            builder.show()



            Toast.makeText(this, muestra.nombreMuestra, Toast.LENGTH_SHORT).show()
            Log.i("Ray", muestra.nombreMuestra)
        }

    }

    private fun onDeletedItem(position: Int) {
        if (modoEdicion == true || muestraMutableList[position].observaciones.contains( "Eliminada" )){
            Toast.makeText(this, "No se puede eliminar una muestra en modo  o eliminada", Toast.LENGTH_SHORT).show()
        }else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmación")
            // Crear un EditText para ingresar el motivo de la eliminación

            val input = EditText(this)
            input.hint = "Ingrese el motivo de la eliminación"
            builder.setView(input)


            builder.setMessage("¿Estás seguro de que deseas eliminar la muestra?")

            builder.setPositiveButton("Sí") { dialog, which ->
                try {

                    val motivoEliminacion = input.text.toString().trim()
                    if (motivoEliminacion.isEmpty()) {
                        Toast.makeText(this, "Debe ingresar un motivo para la eliminación", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val muestraEliminada = muestraMutableList[position]
                    muestraEliminada.observaciones = "Eliminada - Motivo: $motivoEliminacion"

                    val tvCantidad = binding.tvCantidadRestante
                    val servicioAsociado =
                        serviciosList.find { it.id == muestraEliminada.servicioId }
                    val spinner1 = binding.idSpinner1

                    // Verificar si se encontró el servicio asociado
                    if (servicioAsociado != null) {
                        // Incrementar la cantidad del servicio
                        servicioAsociado.cantidad++
                        println(muestraEliminada.servicioId.toString() + "= " + spinner1.selectedItem.toString())
                        if (muestraEliminada.servicioId == spinner1.selectedItem.toString()
                        ) {
                            tvCantidad.text = servicioAsociado.cantidad.toString()
                            if (servicioAsociado.cantidad == 0) {
                                tvCantidad.setTextColor(resources.getColor(R.color.red))
                            }else{
                                tvCantidad.setTextColor(resources.getColor(R.color.green))
                            }
                        }

                    }
                    //No borrarlos
//                    muestraMutableList.removeAt(position)
//                    //Notificar al listado que se ha en este caso borrado un item con una posicion
//                    adapter.notifyItemRemoved(position)
                    val tvFolio = binding.tvFolio



//                    // Actualizar los números de muestra en la lista
//                    for (i in position until muestraMutableList.size) {
//                        muestraMutableList[i].numeroMuestra = (i + 1).toString()
//                        muestraMutableList[i].registroMuestra =
//                            tvFolio.text.toString() + "-" + muestraMutableList[i].numeroMuestra
//                        muestraMutableList[i].idLab = fechaSinBarras+tvFolio.text.toString() + "-" + muestraMutableList[i].numeroMuestra
//
//                    }
                    adapter.notifyItemRangeChanged(position, muestraMutableList.size)

                    // Actualizar contador y TextView de número de muestra
//                    contador = muestraMutableList.size
//                    binding.tvNumeroMuestra.text = (contador + 1).toString()
//                    binding.tvregistromuestra.text =
//                        tvFolio.text.toString() + "-" + binding.tvNumeroMuestra.text.toString()
                    Log.e("Prueba".toString(), "El contador es:$contador")

                    checkStoragePermissionAndSaveJson()


                } catch (e: Exception) {
                    Log.e("Error".toString(), "Hubo un error")
                }

            }

            // Configurar el botón "No"
            builder.setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }

            // Mostrar el cuadro de diálogo
            builder.show()

        }


    }

    private fun onEditItem(position: Int) {
        if (modoEdicion == true || muestraMutableList[position].observaciones.contains( "Eliminada" )){
            Toast.makeText(this, "No se puede elegir editar otra muestra en modo edicion o eliminada", Toast.LENGTH_SHORT).show()
        }else{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmación")
            builder.setMessage("¿Estás seguro de que deseas editar la muestra?")
            builder.setPositiveButton("Sí") { dialog, which ->
                try {
                    setEditMode(true)
                    val servicioSeleccionado = muestraMutableList[position].servicioId
                    for (servicio in serviciosList) {
                        if (servicio.id == servicioSeleccionado) {
                            val spinner1 = binding.idSpinner1
                            spinner1.setSelection(serviciosList.indexOf(servicio))
                            spinner1.isEnabled = false

                            val subtipo = binding.idspinnerSubtipo
                            subtipo.setSelection(subtipos.indexOf(muestraMutableList[position].subtipo))

                            break
                        }
                    }



                    Toast.makeText(this, "Editando la muestra ${muestraMutableList[position].nombreMuestra}", Toast.LENGTH_SHORT).show()
                    binding.tvTitulo.text = "Editando Muestra ${muestraMutableList[position].registroMuestra}"
                    binding.btnStart.text = "Aceptar Edicion"
                    binding.txtnombre.setText(muestraMutableList[position].nombreMuestra)
                    binding.txtcantidadaprox.setText(muestraMutableList[position].cantidadAprox)
                    binding.txtTemp.setText(muestraMutableList[position].tempM)
                    binding.txtLugar.setText(muestraMutableList[position].lugarToma)
                    binding.txtdescripcion.setText(muestraMutableList[position].descripcionM)
                    binding.txtMicro.setText(muestraMutableList[position].emicro)
                    binding.txtFisico.setText(muestraMutableList[position].efisico)
                    binding.txtobservaciones.setText(muestraMutableList[position].observaciones)

                    indexMuestraAEditar = position

                    adapterEdicion = adapter


                }catch (e:Exception){
                    setEditMode(false)
                    Log.e("Error".toString(), "Hubo un error ${e}")
                }
            }
            builder.setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }

            builder.show()
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        //.removeCallbacks(runnable) // Detener la actualización de la hora cuando se destruye la actividad
    }

    private fun checkStoragePermissionAndSaveJson() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + applicationContext.packageName)
                startActivityForResult(intent, storagePermissionRequestCode)
            } else {
                val muestraData = MuestraData(
                    binding.tvFolio.text.toString(),
                    pdmSeleccionado,
                    clientePdm,
                    serviciosList,
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
                    clientePdm,
                    serviciosList,
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
    fun setEditMode(editMode: Boolean) {
        modoEdicion = editMode
        // Aquí puedes hacer otras acciones necesarias cuando el modo edición cambia
    }
    private fun showServicioDialog() {


        val dialogView = layoutInflater.inflate(R.layout.dialog_servicio_list, null)
        val recyclerView: RecyclerView = dialogView.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ServicioAdapterInfo(serviciosList)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Lista de Servicios")
            .setView(dialogView)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()
    }

}