package com.example.aplicacionlesaa

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aplicacionlesaa.MainActivity2.FooterEventHandler
import com.example.aplicacionlesaa.model.MuestraData
import com.example.aplicacionlesaa.worker.SendDataWorker
import com.example.aplicacionlesaa.worker.SendDataWorkerFQ
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.io.source.ByteArrayOutputStream
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File


class fisicoquimicosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFisicoquimicosBinding
    private lateinit var adapter: analisisFisicoAdapter
    private var analisisFisicoList: MutableList<analisisFisico> = mutableListOf()
    private var datosGuardados = false
    private val storagePermissionRequestCode = 1001
    private var folioSolicitud: String = ""


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
        folioSolicitud =  intent.getStringExtra("folioSolicitud") ?: ""
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

                val tamanoFq = analisisFisicoList.size


                //Enviar datos desde el worker
                // Enviar datos desde el worker
                val data = Data.Builder()
                    .putInt("fq_count", tamanoFq)

                // Agregar todos los datos en un solo `Data` en lugar de enviarlos por separado
                analisisFisicoList.forEachIndexed { index, muestra ->
                    data.putString("registro_muestra_$index", muestra.registro_muestra)
                    data.putString("nombre_muestra_$index", muestra.nombre_muestra)
                    data.putString("hora_analisis_$index", muestra.hora_analisis)
                    data.putString("temperatura_$index", muestra.temperatura)
                    data.putString("ph_$index", muestra.ph)
                    data.putString("clr_$index", muestra.clr.toString())
                    data.putString("clt_$index", muestra.clt.toString())
                    data.putString("crnas_$index", muestra.crnas.toString())
                    data.putString("cya_$index", muestra.cya.toString())
                    data.putString("tur_$index", muestra.tur.toString())
                }

                // Crear y enviar las tareas programadas para cada muestra en muestraMutableList
                // Crear y enviar el único trabajo de envío con toda la información
                    val workRequest = OneTimeWorkRequestBuilder<SendDataWorkerFQ>()
                    .setInputData(data.build())
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()

                    Log.i("Si hay internet", "Entre al worker")
                    WorkManager.getInstance(this).enqueue(workRequest)

                checkStoragePermissionAndSavePdf()
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

    private fun savePdf(emailAddress: String) {
        val pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val file = File(pdfPath, "EstudiosFq-Folio-${folioSolicitud}.pdf")

        try {
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument, PageSize.A4.rotate())

            // Manejar encabezados y pies de página
            val footerHandler = FooterEventHandler(document)
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler)

            // Cargar el logotipo
            val inputStream = applicationContext.resources.openRawResource(R.raw.logorectangulartrans)
            val byteArrayOutputStream = ByteArrayOutputStream()
            var nextByte = inputStream.read()
            while (nextByte != -1) {
                byteArrayOutputStream.write(nextByte)
                nextByte = inputStream.read()
            }
            val imageData = byteArrayOutputStream.toByteArray()
            val logo = Image(ImageDataFactory.create(imageData))
            logo.scaleToFit(150f, 100f)

            // Agregar el logotipo al documento
            val headerTable = Table(1).useAllAvailableWidth()
            headerTable.addCell(Cell().add(logo).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.LEFT))
            document.add(headerTable)

            // Agregar título principal
            val title = Paragraph("F-FQ-LAB-02 - Formato de Reporte de Parámetros Fisicoquímicos de Aguas de Albercas")
                .setFontSize(14f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
            document.add(title)

            // Información general
            val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f)))
                .useAllAvailableWidth()
            infoTable.addCell(Cell().add(Paragraph("Nombre del Cliente:").setBold()))
//            infoTable.addCell(Cell().add(Paragraph(binding.tvCliente.text ?: "")))
            infoTable.addCell(Cell().add(Paragraph("Hotel bahia")))
            infoTable.addCell(Cell().add(Paragraph("Dirección:").setBold()))
//            infoTable.addCell(Cell().add(Paragraph(binding.tvDireccion.text ?: ""))
            infoTable.addCell(Cell().add(Paragraph("Tulum tulum")))

            infoTable.addCell(Cell().add(Paragraph("Fecha:").setBold()))
//            infoTable.addCell(Cell().add(Paragraph(binding.tvFecha.text ?: "")))
            infoTable.addCell(Cell().add(Paragraph("10/12/2024")))

            document.add(infoTable)

            // Tabla de parámetros fisicoquímicos
            val columnWidths = floatArrayOf(2f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 2f)
            val paramTable = Table(columnWidths).useAllAvailableWidth()
            val headers = arrayOf(
                "Nombre de la Muestra / Registro", "Hora de Análisis", "TEMP(°C)", "pH",
                "CLR", "CLT", "CRNAS", "CYA", "TUR", "Fe", "Observaciones", "Comentarios"
            )
            headers.forEach { header -> paramTable.addHeaderCell(Cell().add(Paragraph(header).setBold())) }

            // Agregar datos dinámicos a la tabla
            for (i in 1..5) { // Ejemplo de 5 filas
                paramTable.addCell(Cell().add(Paragraph("Muestra $i")))
                paramTable.addCell(Cell().add(Paragraph("12:00 PM")))
                paramTable.addCell(Cell().add(Paragraph("25.0")))
                paramTable.addCell(Cell().add(Paragraph("7.0")))
                paramTable.addCell(Cell().add(Paragraph("1.5")))
                paramTable.addCell(Cell().add(Paragraph("1.8")))
                paramTable.addCell(Cell().add(Paragraph("0.2")))
                paramTable.addCell(Cell().add(Paragraph("0.0")))
                paramTable.addCell(Cell().add(Paragraph("3.5")))
                paramTable.addCell(Cell().add(Paragraph("0.1")))
                paramTable.addCell(Cell().add(Paragraph("Sin observaciones")))
                paramTable.addCell(Cell().add(Paragraph("Comentario ejemplo")))
            }
            document.add(paramTable)

            // Nomenclatura
            val nomenclature = Paragraph("Nomenclatura de Estudios Fisicoquímicos")
                .setBold()
                .setUnderline()
                .setTextAlignment(TextAlignment.LEFT)
            document.add(nomenclature)
            val nomenclatureDetails = Paragraph(
                """
            PH: Potencial de Hidrógeno 
            CLR: Cloro Libre Residual
            CLT: Cloro Total
            CRNAS: Cloraminas
            CYA: Ácido Cianúrico
            TUR: Turbidez
            Fe: Hierro
        """.trimIndent()
            )
            document.add(nomenclatureDetails)
            Log.i("Si hay internet", "Entre al pdf")
            document.close()

        } catch (e: Exception) {
            e.printStackTrace()
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
    private fun checkStoragePermissionAndSavePdf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + applicationContext.packageName)
                startActivityForResult(intent, storagePermissionRequestCode)
            } else {

                savePdf("ray.contacto06@gmail.com")

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

                savePdf("ray.contacto06@gmail.com")

            }
        }
    }


}
