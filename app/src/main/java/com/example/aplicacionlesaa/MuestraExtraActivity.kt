package com.example.aplicacionlesaa

import DragManageAdapter
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
import com.example.aplicacionlesaa.adapter.EstudiosAdapterInfo
import com.example.aplicacionlesaa.adapter.ServicioAdapterInfo
import com.example.aplicacionlesaa.adapter.muestraAdapter
import com.example.aplicacionlesaa.databinding.ActivityMuestraExtraBinding
import com.example.aplicacionlesaa.model.ClientePdm
import com.example.aplicacionlesaa.model.Descripcion
import com.example.aplicacionlesaa.model.Estudio
import com.example.aplicacionlesaa.model.MuestraData
import com.example.aplicacionlesaa.model.Pdm
import com.example.aplicacionlesaa.model.Servicio
import com.example.aplicacionlesaa.utils.OnItemMovedListener
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.ArrayList

class MuestraExtraActivity : AppCompatActivity(), OnItemMovedListener {

    private lateinit var binding: ActivityMuestraExtraBinding
    private lateinit var pdmDetallado: Pdm
    private var descripcionesList: MutableList<Descripcion> = mutableListOf()
    private var clientePdm: ClientePdm? = null
    private var folio : String? = null
    private var arregloDeNumeros: MutableList<Int> = mutableListOf()
    private var estudioSeleccionado: Estudio? = null

    private val estudiosList = mutableListOf<Estudio>(
        Estudio(1, "AGUA CRUDA", "CI-EST-001", "100ml", "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."),
        Estudio(2, "AGUA DE POZO", "CI-EST-002", "100ml", "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."),
        Estudio(3, "AGUA DE FUENTE ORNAMENTAL", "CI-EST-003", "100ml", "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."),
        Estudio(4, "AGUA DE RED", "CI-EST-004", "100ml", "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."),
        Estudio(5, "AGUA DE RIEGO", "CI-EST-005", "100ml", "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."),
        Estudio(6, "HIELO", "CI-EST-006", "100ml", "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."),
        Estudio(7, "AGUA DE OSMOSIS", "CI-EST-007", "100ml", "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."),
        Estudio(8, "AGUA CARBOJET", "CI-EST-008", "100ml", "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."),
        Estudio(9, "AGUA PURIFICADA", "CI-EST-009", "100ml", "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."),
        Estudio(10, "AGUA ALCALINA", "CI-EST-010", "100ml", "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."),
        Estudio(11, "ALIMENTOS COCIDOS", "CI-EST-011", "300gr", "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."),
        Estudio(12, "ALIMENTOS CRUDOS", "CI-EST-012", "300gr", "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."),
        Estudio(13, "SUPERFICIE VIVA", "CI-EST-013", "10ml", "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."),
        Estudio(14, "SUPERFICIE INERTE", "CI-EST-014", "10ml", "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."),
        Estudio(15, "POSTRES", "CI-EST-015", "300gr", "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."),
        Estudio(16, "POSTRES", "CI-EST-016", "300gr", "NOM-247-SSA1-2008, Productos y servicios. Cereales y sus productos. Cereales, harinas de cereales, sémolas o semolinas. Alimentos a base de: cereales, semillas comestibles, de harinas, sémolas o semolinas o sus mezclas. Productos de panificación. Disposiciones y especificaciones sanitarias y nutrimentales. Métodos de prueba."),
        Estudio(17, "ALIMENTO A BASE LÁCTEA", "CI-EST-017", "300gr", "NOM-243-SSA1-2010, Productos y servicios. Leche, fórmula láctea, producto lácteo combinado y derivados lácteos. Disposiciones y especificaciones sanitarias. Métodos de prueba."),
        Estudio(18, "PRODUCTOS DE LA PESCA FRESCOS,REFRIGERADOS,CONGELADOS Y PROCESADOS", "CI-EST-018", "300gr", "NOM-242-SSA1-2009, Productos y servicios. Productos de la pesca frescos, refrigerados, congelados y procesados. Especificaciones sanitarias y métodos de prueba."),
        Estudio(19, "PRODUCTOS CARNICOS PROCESADOS", "CI-EST-019", "300gr", "NOM-213-SSA1-2002, Productos y servicios. Productos cárnicos procesados. Especificaciones sanitarias."),
        Estudio(20, "AGUA DE ALBERCA (Microbiologicos)", "CI-EST-020", "100ml", "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio microbiológico)"),
        Estudio(21, "AGUA DE ALBERCA (Fisicoquimicos)", "CI-EST-021", "100ml", "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio fisicoquimico)"),
        Estudio(22, "AGUA DE JACUZZI (Microbiologicos)", "CI-EST-022", "100ml", "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio microbiológico)"),
        Estudio(23, "AGUA DE JACUZZI (Fisicoquimicos)", "CI-EST-023", "100ml", "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio fisicoquimico)"),
        Estudio(24, "AGUA RESIDUAL", "CI-EST-024", "100ml", "NOM-001-SEMARNAT-2021, Que establece los límites permisibles de contaminantes en las descargas de aguas residuales en cuerpos receptores propiedad de la nación."),
        Estudio(25, "AGUA RESIDUAL", "CI-EST-025", "100ml", "NOM-002-ECOL-1996, Que establece los límites máximos permisibles de contaminantes en las descargas de aguas residuales a los sistemas de alcantarillado urbano o municipal."),
        Estudio(26, "PTAR", "CI-EST-026", "NULL", "NOM-001-SEMARNAT-2021, Que establece los límites permisibles de contaminantes en las descargas de aguas residuales en cuerpos receptores propiedad de la nación."),
        Estudio(27, "PTAR", "CI-EST-027", "NULL", "NOM-001-SEMARNAT-2021, Que establece los límites permisibles de contaminantes en las descargas de aguas residuales en cuerpos receptores propiedad de la nación."),
        Estudio(28, "LODO", "CI-EST-028", "NULL", "NOM-004-SEMARNAT-2002, Protección ambiental.- Lodos y biosólidos.- Especificaciones y límites máximos permisibles de contaminantes para su aprovechamiento y disposición final."),
        Estudio(29, "LEGIONELLA", "CI-EST-029", "NULL", "UNE 100030:2017 Prevención y control de la proliferación y diseminación de Legionella en instalaciones."),
        Estudio(30, "Ma", "CI-EST-030", "NULL", "UNE 100030:2017 Prevención y control de la proliferación y diseminación de Mesofilicos Aerobios en instalaciones."),
        Estudio(31, "SALMONELLA", "CI-EST-031", "NULL", "Perfil de estudio para determinación de Salmonella."),
        Estudio(32, "VIBRIO CHOLERAE", "CI-EST-032", "NULL", "Perfil de estudio para determinación de Vibrio Cholerae."),
        Estudio(33, "HIERRO", "CI-EST-033", "NULL", "Perfil de estudios para determinación de HIERRO."),
        Estudio(34, "E.COLI", "CI-EST-034", "NULL", "Perfil de estudios para determinación de E.COLI."),
        Estudio(35, "STAPHYLOCOCCUS AUREUS", "CI-EST-035", "NULL", "Perfil de estudios para determinación de STAPHYLOCOCCUS."),
        Estudio(36, "SERVICIO DE RECOLECCION DE MUESTRAS", "CI-EST-036", "NULL", "N/A."),
        Estudio(37, "AGUA TRATADA", "CI-EST-037", "100ml", "NOM-003-ECOL-1997, Que establece los límites máximos permisibles de contaminantes para las aguas residuales tratadas que se reusen en servicios al público."),
        Estudio(38, "PROTEINAS CRUDAS", "CI-EST-038", "300gr", "NOM-213-SSA1-2018, Productos y servicios. Productos cárnicos procesados y los establecimientos dedicados a su proceso. Disposiciones y especificaciones sanitarias. Métodos de prueba.; NOM-242-SSA1-2009, Productos de la pesca frescos, refrigerados, congelados y procesados. Especificaciones sanitarias y métodos de prueba.")
    )

    private lateinit var adapterEdicion: muestraAdapter
    private lateinit var adapter: muestraAdapter
    private var modoEdicion = false
    private var muestraMutableList: MutableList<Muestra> = mutableListOf()
    private var fechaSinBarras: String = ""
    private var lugares: ArrayList<String> = ArrayList()
    var contador = 0
    private val storagePermissionRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMuestraExtraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        binding.tvfechamuestreo.text = currentDate

        clientePdm =   intent.getParcelableExtra("clientePdm")
        pdmDetallado = intent.getParcelableExtra("pdmDetallado")!!
        lugares = intent.getStringArrayListExtra("lugares") ?: ArrayList()

        val adapterLugares = ArrayAdapter(this@MuestraExtraActivity, android.R.layout.simple_spinner_dropdown_item, lugares)
        binding.txtLugar.setAdapter(adapterLugares)

        try {
            val muestrasExistentes = intent.getParcelableArrayListExtra<Muestra>("muestraList")
            if (muestrasExistentes != null) {
                muestraMutableList.addAll(muestrasExistentes)
                contador = muestraMutableList.size
                binding.tvNumeroMuestra.text = (contador + 1).toString()
            } else {
                contador = 0
            }
        } catch (e: Exception) {
            Log.e("Error", "Error al obtener la lista de muestras:" + e)
        }

        folio = intent.getStringExtra("folio")
        folio = folio + "E"

        binding.tvFolio.text = folio
        binding.tvCliente.text = clientePdm?.nombre_empresa
        binding.tvPDM.text = pdmDetallado.nombre_pdm
        binding.tvregistromuestra.text = folio + "-" + binding.tvNumeroMuestra.text.toString()

        for (x in 1..38){
            arregloDeNumeros.add(x)
        }

        binding.btnStart.setOnClickListener {
            if (modoEdicion) {
                // Lógica de edición pendiente
            } else {
                if (estudioSeleccionado == null) {
                    Toast.makeText(this, "Por favor selecciona un estudio", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val sepudo = createMuestra()
                if (sepudo) {
                    binding.tvregistromuestra.text = binding.tvFolio.text.toString() + "-" + binding.tvNumeroMuestra.text.toString()
                    clearTextFields()
                }
                checkStoragePermissionAndSaveJson()
            }
        }

        binding.btnAceptar.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmación")
            builder.setMessage("¿Estás seguro de que deseas guardar las muestras extras?")
            builder.setPositiveButton("Sí") { dialog, which ->
                checkStoragePermissionAndSaveJson()
                val resultIntent = Intent()
                resultIntent.putParcelableArrayListExtra("muestrasList", ArrayList(muestraMutableList))
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
            builder.setNegativeButton("No") { dialog, which -> dialog.dismiss() }
            builder.show()
        }

        binding.btnInfo.setOnClickListener{
            showServicioDialog()
        }

        binding.btnCancelar.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmación")
            builder.setMessage("¿Estás seguro de que deseas salir sin guardar?")
            builder.setPositiveButton("Ok") { dialog, which -> finish() }
            builder.setNegativeButton("No") { dialog, which -> dialog.dismiss() }
            builder.show()
        }

        val descripcionesLista = intent.getParcelableArrayListExtra<Descripcion>("descripciones")
        if (descripcionesLista != null) {
            descripcionesList.addAll(descripcionesLista)
        }

        val descris = descripcionesList.map { it.descripcion.toString() }
        val adapterDesci = ArrayAdapter(this@MuestraExtraActivity, android.R.layout.simple_spinner_dropdown_item, descris)
        binding.txtdescripcion.setAdapter(adapterDesci)
        
        binding.txtLugar.setOnClickListener { binding.txtLugar.showDropDown() }
        binding.txtdescripcion.setOnClickListener { binding.txtdescripcion.showDropDown() }
        
        // Sincronización del Spinner con la selección visual
        val spinner = binding.idSpinner1
        val adapterSpinner = ArrayAdapter(this@MuestraExtraActivity, android.R.layout.simple_spinner_item, arregloDeNumeros)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapterSpinner

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedId = parent?.getItemAtPosition(position) as Int
                val estudio = estudiosList.find { it.id == selectedId }
                if (estudio != null) {
                    actualizarCamposEstudio(estudio)
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        initRecyclerView()
    }

    private fun actualizarCamposEstudio(estudio: Estudio) {
        estudioSeleccionado = estudio
        binding.tvdescripcionmuestra.text = "${estudio.clasificacion}-${estudio.norma}"
        binding.txtcantidadaprox.setText(estudio.cantidad_toma)
        
        // Rellenado automático del nombre según clasificación
        binding.txtFisico.isEnabled = true
        val clasif = estudio.clasificacion.uppercase()
        
        if (clasif.contains("ALBERCA") || clasif.contains("JACUZZI") || clasif.contains("RECREACTIVO")) {
            binding.txtnombre.setText("Agua de Alberca")
            binding.txtFisico.isEnabled = false
        } else if (clasif.contains("AGUA DE RED")) {
            binding.txtnombre.setText("Agua de Red")
        } else if (clasif.contains("HIELO")) {
            binding.txtnombre.setText("Hielo")
        } else if (clasif.contains("AGUA DE RIEGO")) {
            binding.txtnombre.setText("Agua de Riego")
        } else if (clasif.contains("AGUA RESIDUAL")) {
            binding.txtnombre.setText("Agua Residual")
        } else if (clasif.contains("VIVA")) {
            binding.txtnombre.setText("Superficie Viva")
        } else if (clasif.contains("INERTE")) {
            binding.txtnombre.setText("Superficie Inerte")
        } else {
            binding.txtnombre.text.clear()
        }
        
        // Sincronizar el spinner visualmente
        binding.idSpinner1.setSelection(estudiosList.indexOf(estudio))
    }

    private fun showServicioDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_servicio_list, null)
        val recyclerView: RecyclerView = dialogView.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Lista de Estudios")
            .setView(dialogView)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .create()

        recyclerView.adapter = EstudiosAdapterInfo(estudiosList) { estudio ->
            actualizarCamposEstudio(estudio)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createMuestra(): Boolean {
        if (binding.txtnombre.text.toString().trim().isEmpty() || 
            binding.txtcantidadaprox.text.toString().trim().isEmpty() || 
            binding.txtTemp.text.toString().trim().isEmpty() || 
            binding.txtLugar.text.toString().trim().isEmpty() || 
            binding.txtdescripcion.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            return false
        }

        val numeroMuestra = binding.tvNumeroMuestra.text.toString()
        val formatoEntrada = SimpleDateFormat("MM/dd/yyyy")
        val formatoSalida = SimpleDateFormat("yyyyMMdd")
        val fecha = formatoEntrada.parse(binding.tvfechamuestreo.text.toString())
        fechaSinBarras = formatoSalida.format(fecha)
        val idLab = fechaSinBarras + binding.tvregistromuestra.text.toString()

        val muestraobjeto = Muestra(
            numeroMuestra = numeroMuestra,
            fechaMuestra = binding.tvfechamuestreo.text.toString(),
            registroMuestra = binding.tvregistromuestra.text.toString(),
            nombreMuestra = binding.txtnombre.text.toString().trim(),
            idLab = idLab,
            cantidadAprox = binding.txtcantidadaprox.text.toString().trim(),
            tempM = binding.txtTemp.text.toString().trim() + "°C",
            lugarToma = binding.txtLugar.text.toString().trim(),
            descripcionM = binding.txtdescripcion.text.toString().trim(),
            emicro = binding.txtMicro.text.toString().trim(),
            efisico = binding.txtFisico.text.toString().trim(),
            observaciones = binding.txtobservaciones.text.toString().trim(),
            servicioId = "",
            idEstudio = estudioSeleccionado?.id.toString()
        )
        
        muestraMutableList.add(muestraobjeto)
        contador = muestraMutableList.size
        binding.tvNumeroMuestra.text = (contador + 1).toString()
        adapter.notifyItemInserted(muestraMutableList.size - 1)
        Toast.makeText(this, "Se ha añadido la muestra", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun initRecyclerView() {
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
        checkStoragePermissionAndSaveJson()
    }

    private fun onItemSelected(muestra: Muestra) {
        if (modoEdicion) {
            Toast.makeText(this, "No se puede copiar en modo edicion", Toast.LENGTH_SHORT).show()
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmación")
            builder.setMessage("¿Estás seguro de que deseas copiar la muestra ${muestra.nombreMuestra}?")
            builder.setPositiveButton("Sí") { dialog, which ->
                clearTextFields()
                binding.txtnombre.setText(muestra.nombreMuestra)
                binding.txtTemp.setText(muestra.tempM.replace("°C", ""))
                binding.txtLugar.setText(muestra.lugarToma)
                binding.txtdescripcion.setText(muestra.descripcionM)
            }
            builder.setNegativeButton("No") { dialog, which -> dialog.dismiss() }
            builder.show()
        }
    }

    private fun onDeletedItem(position: Int) {
        if (modoEdicion || muestraMutableList[position].observaciones.contains("Eliminada")) {
            Toast.makeText(this, "No se puede eliminar", Toast.LENGTH_SHORT).show()
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmación")
            val input = EditText(this)
            input.hint = "Ingrese el motivo"
            builder.setView(input)
            builder.setMessage("¿Deseas eliminar la muestra?")
            builder.setPositiveButton("Sí") { dialog, which ->
                val motivo = input.text.toString().trim()
                if (motivo.isEmpty()) {
                    Toast.makeText(this, "Motivo requerido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                muestraMutableList[position].observaciones = "Eliminada - Motivo: $motivo"
                adapter.notifyItemChanged(position)
                checkStoragePermissionAndSaveJson()
            }
            builder.setNegativeButton("No") { dialog, which -> dialog.dismiss() }
            builder.show()
        }
    }

    private fun checkStoragePermissionAndSaveJson() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + applicationContext.packageName)
                startActivityForResult(intent, storagePermissionRequestCode)
            } else {
                saveJson()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), storagePermissionRequestCode)
            } else {
                saveJson()
            }
        }
    }

    private fun saveJson() {
        val muestraData = MuestraData(
            binding.tvFolio.text.toString(),
            pdmDetallado.nombre_pdm,
            clientePdm,
            emptyList(),
            muestraMutableList,
            pdmDetallado,
            ArrayList()
        )
        saveDataToJson(this, muestraData, "Datos-folioExtra-${binding.tvFolio.text}.json")
    }

    private fun clearTextFields() {
        binding.txtnombre.text.clear()
        binding.txtTemp.text.clear()
        binding.txtLugar.text.clear()
        binding.txtdescripcion.text.clear()
        binding.txtobservaciones.text.clear()
        binding.txtMicro.text.clear()
        binding.txtFisico.text.clear()
    }

    fun saveDataToJson(context: Context, muestraData: MuestraData, filename: String) {
        val jsonString = Gson().toJson(muestraData)
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), filename)
        file.writeText(jsonString)
    }

    fun setEditMode(editMode: Boolean) { modoEdicion = editMode }
    private fun onEditItem(position: Int) { Toast.makeText(this, "En desarrollo",Toast.LENGTH_SHORT).show() }
}