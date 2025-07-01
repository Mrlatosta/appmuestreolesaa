package com.example.aplicacionlesaa

import android.Manifest
import android.bluetooth.BluetoothClass.Device
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import com.example.aplicacionlesaa.model.ClientePdm
import com.example.aplicacionlesaa.model.MuestraData
import com.example.aplicacionlesaa.worker.SendDataWorker
import com.example.aplicacionlesaa.worker.SendDataWorkerFQ
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.io.source.ByteArrayOutputStream
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.text.format


class fisicoquimicosActivity : AppCompatActivity(),SignatureDialogFragment.SignatureDialogListener {

    private lateinit var binding: ActivityFisicoquimicosBinding
    private lateinit var adapter: analisisFisicoAdapter
    private var analisisFisicoList: MutableList<analisisFisico> = mutableListOf()
    private var datosGuardados = false
    private val storagePermissionRequestCode = 1001
    private var folioSolicitud: String = ""
    private var nombreCliente: String = ""
    private var clientePdm: ClientePdm? = null


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
        nombreCliente = intent.getStringExtra("ClienteNombre") ?: ""
        binding.tvFolioSolicitudFQ.text = "Folio solicitud: $folioSolicitud"
        binding.tvNombreClienteFQ.text = "Cliente: $nombreCliente"
        clientePdm = intent.getParcelableExtra("clientePdm")


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


        binding.btnInsertSignature.setOnClickListener {
            //Mostrarle una ventana de informacion antes de proceder
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Información")
            builder.setMessage("Al firmar, usted confirma que los lugares de toma de muestra o centros de consumo proporcionados son correctos. Asimismo, se le informa que podrán realizarse modificaciones en la información de las muestras en caso de errores ortográficos, y cualquier cambio derivado de esta situación le será notificado debidamente.")
            builder.setPositiveButton("Aceptar") { dialog, which ->
                // Acción a realizar al hacer clic en "Aceptar"
                val signatureDialog = SignatureDialogFragment()
                signatureDialog.setSignatureDialogListener(this)
                signatureDialog.show(supportFragmentManager, "SignatureDialogFragment")
            }
            val dialog = builder.create()
            dialog.show()


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
    //checando lo de la tablet
            //#002060 - 0 32 96 - Azul obscuro arriba borde
            //#002060 - 0 112 192 - Azul Cielo abajo borde

            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument, PageSize.A4.rotate())

            // Manejar encabezados y pies de página
            val footerHandler = FooterEventHandler(document)
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler)

            // Cargar el logotipo
            val inputStream = applicationContext.resources.openRawResource(R.raw.logorectangulartranssinabajo)
            val byteArrayOutputStream = ByteArrayOutputStream()
            var nextByte = inputStream.read()
            while (nextByte != -1) {
                byteArrayOutputStream.write(nextByte)
                nextByte = inputStream.read()
            }
            val imageData = byteArrayOutputStream.toByteArray()
            val logo = Image(ImageDataFactory.create(imageData))
            logo.scaleToFit(180f, 30f)

            // Agregar el logotipo al documento
            val headerTable = Table(2).useAllAvailableWidth()
            headerTable.addCell(Cell().add(logo).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.LEFT)).setTextAlignment(TextAlignment.CENTER)
            headerTable.addCell(Cell().add(Paragraph("Centro Integral en Servicios de Laboratorio de Agua y Alimentos S.A de C.V")).setTextAlignment(TextAlignment.CENTER).setBorder(Border.NO_BORDER).setBold()
                .setBorderTop(SolidBorder(DeviceRgb(0, 32,96), 6f)
            ).setBorderBottom(SolidBorder(DeviceRgb(0, 112,192), 6f)).setTextAlignment(TextAlignment.CENTER)
            )
            document.add(headerTable)

            // Agregar título principal
            val title = Paragraph("F-FQ-LAB-02 - Formato de Reporte de Parámetros Fisicoquímicos de Aguas de Albercas")
                .setFontSize(14f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)

//            document.add(title)

            val mediumTable = Table(4).useAllAvailableWidth().setBorderTop(SolidBorder(DeviceRgb(237,125,49),6f)).setBorderBottom(SolidBorder(DeviceRgb(237,125,49),6f)).setFontSize(10f)
            mediumTable.addCell(Cell().add(Paragraph("Numero control:")).setPaddings(0f,0f,0f,0f))
            mediumTable.addCell(Cell(1,3).add(Paragraph("F-FQ-LAB-02.- Formato de Reporte de Parámetros Fisicoquimicos de Aguas de Albercas").setBold()))

            mediumTable.addCell(Cell().add(Paragraph("Revision:")).setPaddings(0f,0f,0f,0f))
            mediumTable.addCell(Cell().add(Paragraph("0:").setBold()).setPaddings(0f,0f,0f,0f))
            mediumTable.addCell(Cell().add(Paragraph("Sustituya a:")).setPaddings(0f,0f,0f,0f))
            mediumTable.addCell(Cell().add(Paragraph("Documento nuevo").setBold()).setPaddings(0f,0f,0f,0f))

            mediumTable.addCell(Cell().add(Paragraph("Vigente a partir de:")).setPaddings(0f,0f,0f,0f))
            mediumTable.addCell(Cell().add(Paragraph("Agosto 2024").setBold()).setPaddings(0f,0f,0f,0f))
            mediumTable.addCell(Cell().add(Paragraph("Próxima revisión:")).setPaddings(0f,0f,0f,0f))
            mediumTable.addCell(Cell().add(Paragraph("Agosto 2025").setBold()).setPaddings(0f,0f,0f,0f))
            mediumTable.addCell(Cell().add(Paragraph("Tipo de documento:")).setPaddings(0f,0f,0f,0f))
            mediumTable.addCell(Cell(1,3).add(Paragraph("Formato").setBold()).setPaddings(0f,0f,0f,0f))

            document.add(mediumTable)

            document.add(Paragraph("").setMarginTop(10f)) // 10f representa el margen superior en puntos


            // Información general
            val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f,1f,1f,1f,1f,1f)))
                .useAllAvailableWidth()
            infoTable.addCell(Cell(1,2).add(Paragraph("Nombre del Cliente:").setBold()))
            infoTable.addCell(Cell(1,4).add(Paragraph(nombreCliente)))
//            infoTable.addCell(Cell().add(Paragraph("Hotel bahia")))
            infoTable.addCell(Cell(1,1).add(Paragraph("Dirección:").setBold()))
//            infoTable.addCell(Cell().add(Paragraph(binding.tvDireccion.text ?: ""))
            infoTable.addCell(Cell(1,3).add(Paragraph(clientePdm?.direccion ?: "")))

            infoTable.addCell(Cell(1,1).add(Paragraph("Fecha:").setBold()))
//            infoTable.addCell(Cell().add(Paragraph(binding.tvFecha.text ?: "")))
            val currentDate = getCurrentDateInDdMmYyyyModern()
            infoTable.addCell(Cell(1,1).add(Paragraph(currentDate)))

            infoTable.addCell(Cell(1,1).add(Paragraph("Atencion A:").setBold()))
            infoTable.addCell(Cell(1,1).add(Paragraph(clientePdm?.atencion ?:"")))
            infoTable.addCell(Cell(1,1).add(Paragraph("Puesto:").setBold()))
            infoTable.addCell(Cell(1,1).add(Paragraph(clientePdm?.puesto ?:"")))
            infoTable.addCell(Cell(1,1).add(Paragraph("Folio Solicitud:").setBold()))
            infoTable.addCell(Cell(1,1).add(Paragraph(folioSolicitud)))





            document.add(infoTable)

            // Tabla de parámetros fisicoquímicos
            val columnWidths = floatArrayOf(2f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)
            val paramTable = Table(columnWidths).useAllAvailableWidth()
            val headers = arrayOf(
                "Nombre de la Muestra / Registro", "Hora de Análisis", "TEMP(°C)", "pH",
                "CLR (mg/L)", "CLT (mg/L)", "CRNAS (mg/L)", "CYA (mg/L)", "TUR (NTU)")

            headers.forEach { header -> paramTable.addHeaderCell(Cell().add(Paragraph(header).setBold())) }

            // Agregar datos dinámicos a la tabla
            for (fisico in analisisFisicoList){ // Ejemplo de 5 filas
                paramTable.addCell(Cell().add(Paragraph("Muestra ${fisico.registro_muestra} - ${fisico.nombre_muestra}")))
                paramTable.addCell(Cell().add(Paragraph("${fisico.hora_analisis}")))
                paramTable.addCell(Cell().add(Paragraph(fisico.temperatura)))
                paramTable.addCell(Cell().add(Paragraph(fisico.ph)))
                paramTable.addCell(Cell().add(Paragraph("${fisico.clr}")))
                paramTable.addCell(Cell().add(Paragraph("${fisico.clt}")))
                paramTable.addCell(Cell().add(Paragraph("${fisico.crnas}")))
                paramTable.addCell(Cell().add(Paragraph("${fisico.cya}")))
                paramTable.addCell(Cell().add(Paragraph("${fisico.tur}")))
            }
            paramTable.addCell(Cell(1,1).add(Paragraph("Observaciones generales:")))
            paramTable.addCell(Cell(1,9).add(Paragraph("")))

            paramTable.addCell(Cell(1,5).add(Paragraph("Nomenclatura de Estudios Fisicoquímicos:")))
            paramTable.addCell(Cell(1,5).add(Paragraph("Nombre, Firma y Puesto:")))

            val subtabla = Table(2).useAllAvailableWidth().setBorder(Border.NO_BORDER)
            subtabla.addCell(Cell().add(Paragraph("PH: Potencial de Hidrógeno \n" +
                    "            CLR: Cloro Libre Residual\n" +
                    "            CLT: Cloro Total")).setBorder(Border.NO_BORDER).setFontSize(10f))
            subtabla.addCell(Cell().add(Paragraph("CRNAS: Cloraminas\n" +
                    "            CYA: Ácido Cianúrico\n" +
                    "            TUR: Turbidez\n")).setBorder(Border.NO_BORDER).setFontSize(10f))

            paramTable.addCell(Cell(3,5).add(subtabla))

            paramTable.addCell(Cell(3,5).add(Paragraph("")))

            document.add(paramTable)

            //El registro de los datos en este formato se realiza con letra legible, y con lapicero de color azul, no se acepta registros c
            document.add(Paragraph("El registro de los datos en este formato se realiza con letra legible, y con lapicero de color azul, no se acepta registros con lápiz, rayaduras y tachaduras.\n").setTextAlignment(TextAlignment.CENTER).setFontSize(0.5f))

            // Nomenclaturag

            document.add(Paragraph("DOCUMENTO CONTROLADO\n").setBold().setTextAlignment(TextAlignment.CENTER))

            document.add(Paragraph("Documento propiedad de Centro Integral en Servicios de Laboratorio de Aguas y Alimentos S.A . de C.V. \n" +
                    " no puede reproducirse en forma parcial o total, sin la previa autorización del Laboratorio\n").setTextAlignment(TextAlignment.CENTER))

            Log.i("Si hay internet", "Entre al pdf")
            document.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrentDateInDdMmYyyyModern(): String {
        val currentDate = LocalDate.now() // Gets the current date
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
        return currentDate.format(formatter)
    }


    private fun initializeAnalisisFisicoList() {
        val muestraMutableList = intent.getParcelableArrayListExtra<Muestra>("muestraList") ?: mutableListOf()
        val serviciosList = intent.getParcelableArrayListExtra<Servicio>("listaServicios") ?: mutableListOf()


        for (muestra in muestraMutableList) {

            //Encontrar el servicio correspondiente a la muestra
            val servicio = serviciosList.find { it.id == muestra.servicioId }

            try{
                if (servicio != null) {
                    val emicro = muestra.emicro
                    val regex = Regex("agua de alberca", RegexOption.IGNORE_CASE)
                    val regexFisico = Regex("fq|FQ|fQ", RegexOption.IGNORE_CASE)

                    Log.e("Emicro","Emicro: $emicro")
                    Log.e("Servicio","Descriçiom: ${servicio.descripcion}")

                    if (regex.containsMatchIn(servicio.descripcion) && regexFisico.containsMatchIn(emicro)) {
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

    override fun onSignatureSaved(bitmap: Bitmap) {
        var signatureView = binding.signatureViewDos3
        signatureView.setSignatureBitmap(bitmap)
    }


}
