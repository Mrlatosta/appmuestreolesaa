package com.example.aplicacionlesaa

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aplicacionlesaa.adapter.FoliosAdapter
import com.example.aplicacionlesaa.adapter.MuestraResumenAdapter
import com.example.aplicacionlesaa.databinding.ActivityFirmaFinalBinding
import com.example.aplicacionlesaa.model.DatosFirmaPlan
import com.example.aplicacionlesaa.model.MuestraData
import com.example.aplicacionlesaa.model.Muestra_pdm
import com.example.aplicacionlesaa.model.Muestra_pdmExtra
import SendEmailWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itextpdf.barcodes.BarcodeQRCode
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.io.source.ByteArrayOutputStream
import com.itextpdf.kernel.colors.DeviceCmyk
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.events.Event
import com.itextpdf.kernel.events.IEventHandler
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
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDate

class MainActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityFirmaFinalBinding
    private val pdmMap: MutableMap<String, MutableList<MuestraData>> = mutableMapOf()
    private val planesHoy: MutableList<String> = mutableListOf()
    private var selectedPlan: String? = null
    private lateinit var foliosAdapter: FoliosAdapter
    private lateinit var muestraAdapter: MuestraResumenAdapter
    private var coroutineJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFirmaFinalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclers()
        cargarArchivosHoyAgrupados()

        if (savedInstanceState != null) {
            selectedPlan = savedInstanceState.getString("selectedPlan")
            if (selectedPlan != null) {
                mostrarFoliosDelPlan(selectedPlan!!)
            }
        }

        binding.btnFirmarPlan.setOnClickListener {
            val foliosSeleccionados = foliosAdapter.getSelectedItems()
            if (foliosSeleccionados.isEmpty()) {
                Toast.makeText(this, "Selecciona al menos un folio para firmar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mostrarDialogoFirma(foliosSeleccionados)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineJob?.cancel()
    }

    private fun setupRecyclers() {
        muestraAdapter = MuestraResumenAdapter(emptyList())
        binding.recyclerMuestras.layoutManager = LinearLayoutManager(this)
        binding.recyclerMuestras.adapter = muestraAdapter

        foliosAdapter = FoliosAdapter(emptyList()) { muestraData ->
            val ultimoSeleccionado = foliosAdapter.getSelectedItems().lastOrNull()
            muestraAdapter.updateData(ultimoSeleccionado?.muestras ?: emptyList())
        }
        binding.recyclerFolios.layoutManager = LinearLayoutManager(this)
        binding.recyclerFolios.adapter = foliosAdapter
    }

    private fun cargarArchivosHoyAgrupados() {
        val directorio = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!directorio.exists()) return

        val fechaHoy = LocalDate.now().toString()
        val archivosHoy = directorio.listFiles { file ->
            file.extension == "json" && file.name.contains(fechaHoy)
        }

        pdmMap.clear()
        planesHoy.clear()

        archivosHoy?.forEach { archivo ->
            try {
                val uri = Uri.fromFile(archivo)
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = InputStreamReader(inputStream)
                    val type = object : TypeToken<MuestraData>() {}.type
                    val data: MuestraData = Gson().fromJson(reader, type)
                    val pdm = data.planMuestreo
                    if (!pdmMap.containsKey(pdm)) {
                        pdmMap[pdm] = mutableListOf()
                        planesHoy.add(pdm)
                    }
                    pdmMap[pdm]?.add(data)
                }
            } catch (e: Exception) {
                Log.e("MainActivity2", "Error reading file: ${archivo.name}", e)
            }
        }
        cargarTablaPlanes()
    }

    private fun cargarTablaPlanes() {
        val table = binding.tablePlanes
        while (table.childCount > 1) { table.removeViewAt(1) }
        for (pdm in planesHoy) {
            val folios = pdmMap[pdm] ?: continue
            val cliente = folios.first().clientePdm?.nombre_empresa ?: "-"
            val row = TableRow(this).apply {
                setOnClickListener { mostrarFoliosDelPlan(pdm) }
            }
            row.addView(createCell(pdm))
            row.addView(createCell(cliente))
            row.addView(createCell(folios.size.toString()))
            row.addView(createInfoCell(pdm))
            table.addView(row)
        }
    }

    private fun createCell(text: String): TextView = TextView(this).apply {
        this.text = text
        setPadding(16, 16, 16, 16)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun createInfoCell(pdm: String): View = ImageView(this).apply {
        setImageResource(android.R.drawable.ic_menu_view)
        setPadding(16, 16, 16, 16)
        setColorFilter(Color.parseColor("#4CAF50"))
        setOnClickListener { mostrarFoliosDelPlan(pdm) }
    }

    private fun mostrarFoliosDelPlan(pdm: String) {
        selectedPlan = pdm
        val foliosDelPlan = pdmMap[pdm] ?: emptyList()
        binding.tvPlanSeleccionado.text = "Plan seleccionado: $pdm"
        foliosAdapter.updateData(foliosDelPlan)
        muestraAdapter.updateData(emptyList())
    }

    private fun mostrarDialogoFirma(foliosSeleccionados: List<MuestraData>) {
        val builder = AlertDialog.Builder(this)

        // Crear un layout simple con EditTexts para capturar la información
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 40)

        val edNombreAutoriza = EditText(this)
        edNombreAutoriza.hint = "Nombre quien autoriza"
        layout.addView(edNombreAutoriza)

        val edPuestoAutoriza = EditText(this)
        edPuestoAutoriza.hint = "Puesto quien autoriza"
        layout.addView(edPuestoAutoriza)

        val edNombreMuestreador = EditText(this)
        edNombreMuestreador.hint = "Nombre muestreador"
        layout.addView(edNombreMuestreador)

        val edPuestoMuestreador = EditText(this)
        edPuestoMuestreador.hint = "Puesto muestreador"
        layout.addView(edPuestoMuestreador)

        val edCorreo = EditText(this)
        edCorreo.hint = "Correo electrónico"
        edCorreo.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        edCorreo.setText(foliosSeleccionados.firstOrNull()?.clientePdm?.correo ?: "")
        layout.addView(edCorreo)

        builder.setView(layout)
        builder.setTitle("Datos para firmar plan")
        builder.setMessage("Ingresa los datos necesarios. Las firmas se solicitarán después.")

        builder.setPositiveButton("Siguiente") { dialog, _ ->
            // Primero capturar los datos
            val nombreAutoriza = edNombreAutoriza.text.toString()
            val puestoAutoriza = edPuestoAutoriza.text.toString()
            val nombreMuestreador = edNombreMuestreador.text.toString()
            val puestoMuestreador = edPuestoMuestreador.text.toString()
            val correo = edCorreo.text.toString()

            if (nombreAutoriza.isEmpty() || puestoAutoriza.isEmpty() ||
                nombreMuestreador.isEmpty() || puestoMuestreador.isEmpty() || correo.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // Ahora capturar las firmas
            capturarFirmas(foliosSeleccionados, nombreAutoriza, puestoAutoriza,
                nombreMuestreador, puestoMuestreador, correo)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun capturarFirmas(
        foliosSeleccionados: List<MuestraData>,
        nombreAutoriza: String,
        puestoAutoriza: String,
        nombreMuestreador: String,
        puestoMuestreador: String,
        correo: String
    ) {
        // Diálogo para firma de quien autoriza
        val dialogAutoriza = AlertDialog.Builder(this)
        val signatureViewAutoriza = SignatureView(this)
        signatureViewAutoriza.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            650
        )

        val layoutAutoriza = android.widget.LinearLayout(this)
        layoutAutoriza.orientation = android.widget.LinearLayout.VERTICAL
        layoutAutoriza.addView(signatureViewAutoriza)

        val btnLimpiarAutoriza = android.widget.Button(this)
        btnLimpiarAutoriza.text = "Limpiar"
        btnLimpiarAutoriza.setOnClickListener { signatureViewAutoriza.clear() }
        layoutAutoriza.addView(btnLimpiarAutoriza)

        dialogAutoriza.setView(layoutAutoriza)
        dialogAutoriza.setTitle("Firma de quien autoriza - $nombreAutoriza")
        dialogAutoriza.setMessage("Puesto: $puestoAutoriza\n\nAl firmar, usted confirma que los lugares de toma de muestra o centros de consumo proporcionados son correctos. Asimismo, se le informa que podrán realizarse modificaciones en la información de las muestras en caso de errores ortográficos, y cualquier cambio derivado de esta situación le será notificado debidamente.")

        dialogAutoriza.setPositiveButton("Siguiente") { dialog, _ ->
            val firmaAutoriza = signatureViewAutoriza.getSignatureBitmap()
            dialog.dismiss()

            // Diálogo para firma del muestreador
            val dialogMuestreador = AlertDialog.Builder(this)
            val signatureViewMuestreador = SignatureView(this)
            signatureViewMuestreador.layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                800
            )

            val layoutMuestreador = android.widget.LinearLayout(this)
            layoutMuestreador.orientation = android.widget.LinearLayout.VERTICAL
            layoutMuestreador.addView(signatureViewMuestreador)

            val btnLimpiarMuestreador = android.widget.Button(this)
            btnLimpiarMuestreador.text = "Limpiar"
            btnLimpiarMuestreador.setOnClickListener { signatureViewMuestreador.clear() }
            layoutMuestreador.addView(btnLimpiarMuestreador)

            dialogMuestreador.setView(layoutMuestreador)
            dialogMuestreador.setTitle("Firma del muestreador - $nombreMuestreador")
            dialogMuestreador.setMessage("Puesto: $puestoMuestreador\n\nRecordatorio: Verifica que los lugares de toma, los nombres y las descripciones de las muestras sean correctos")

            dialogMuestreador.setPositiveButton("Generar PDFs") { dialog2, _ ->
                val firmaMuestreador = signatureViewMuestreador.getSignatureBitmap()

                val datosFirma = DatosFirmaPlan(
                    nombreAutoAnalisis = nombreAutoriza,
                    puestoAutoAnalisis = puestoAutoriza,
                    nombreMuestreador = nombreMuestreador,
                    puestoMuestreador = puestoMuestreador,
                    correo = correo,
                    firmaAutoriza = firmaAutoriza,
                    firmaMuestreador = firmaMuestreador
                )

                generarPDFsFirmados(datosFirma, foliosSeleccionados)
                dialog2.dismiss()
            }

            dialogMuestreador.setNegativeButton("Cancelar") { dialog2, _ -> dialog2.dismiss() }
            dialogMuestreador.create().show()
        }

        dialogAutoriza.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        dialogAutoriza.create().show()
    }

    private fun generarPDFsFirmados(datosFirma: DatosFirmaPlan, foliosSeleccionados: List<MuestraData>) {
        // Mostrar diálogo de progreso
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Generando PDFs")
            .setMessage("Procesando ${foliosSeleccionados.size} folio(s)...")
            .setCancelable(false)
            .show()

        // Procesar en segundo plano sin bloquear UI
        coroutineJob = CoroutineScope(Dispatchers.Default).launch {
            val resultados = mutableListOf<Pair<String, Boolean>>() // Folio, éxito

            try {
                for ((index, muestraData) in foliosSeleccionados.withIndex()) {
                    try {
                        savePdfPlan(this@MainActivity2, muestraData, datosFirma)
                        resultados.add(Pair(muestraData.folio, true))
                        Log.d("MainActivity2", "PDF generado exitosamente: ${muestraData.folio}")
                    } catch (e: Exception) {
                        resultados.add(Pair(muestraData.folio, false))
                        Log.e("MainActivity2", "Error generando PDF para folio ${muestraData.folio}", e)
                    }
                }

                // Volver a UI thread para mostrar resultados
                runOnUiThread {
                    progressDialog.dismiss()
                    val exitosos = resultados.count { it.second }
                    val fallidos = resultados.size - exitosos

                    mostrarOpcionesDeEnvio(datosFirma, foliosSeleccionados, exitosos, fallidos, resultados)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Log.e("MainActivity2", "Error general al generar PDFs", e)
                    Toast.makeText(
                        this@MainActivity2,
                        "Error al generar PDFs: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun savePdfPlan(context: Context, muestraData: MuestraData, datosFirma: DatosFirmaPlan) {
        val pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!pdfPath.exists()) {
            pdfPath.mkdirs()
        }

        val file = File(pdfPath, "Muestras-Folio-${muestraData.folio}.pdf")

        try {
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument, PageSize.A4.rotate())

            val headerColor = DeviceCmyk(99, 38, 0, 67)
            val whiteColor = DeviceRgb(255, 255, 255)
            val fontSize = 8f

            // Logo
            val inputStream = context.resources.openRawResource(R.raw.logorectangulartrans)
            val byteArrayOutputStream = ByteArrayOutputStream()
            var nextByte = inputStream.read()
            while (nextByte != -1) {
                byteArrayOutputStream.write(nextByte)
                nextByte = inputStream.read()
            }
            val imageData = byteArrayOutputStream.toByteArray()
            val image = Image(ImageDataFactory.create(imageData)).scaleToFit(125f, 90f)

            // QR Code
            val qrImage = crearQrImage(muestraData, pdfDocument)

            // Encabezado
            val mainTable = Table(UnitValue.createPercentArray(floatArrayOf(4f, 1f, 1f))).useAllAvailableWidth().setBorder(Border.NO_BORDER)
            val tableEncabezado = Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f))).useAllAvailableWidth().setBorder(Border.NO_BORDER)

            tableEncabezado.addCell(Cell(1, 2).add(Paragraph("F-ING-LAB-02. HOJA DE REGISTRO DE CAMPO").setFontColor(whiteColor).setFontSize(fontSize).setBold())
                .setBackgroundColor(headerColor).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE).setBorder(Border.NO_BORDER))
            tableEncabezado.addCell(Cell(1, 2).add(Paragraph("Servicios que generan valor").setFontColor(whiteColor).setFontSize(fontSize).setBold())
                .setBackgroundColor(headerColor).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE).setBorder(Border.NO_BORDER))

            val datosSolicitudTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
            datosSolicitudTable.addCell(Cell().add(Paragraph("AÑO:").setFontSize(fontSize)).setFontColor(whiteColor).setBackgroundColor(headerColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(LocalDate.now().year.toString()).setFontSize(fontSize).setBold()))
            datosSolicitudTable.addCell(Cell().add(Paragraph("MES:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(String.format("%02d", LocalDate.now().monthValue)).setFontSize(fontSize).setBold()))
            datosSolicitudTable.addCell(Cell().add(Paragraph("DÍA:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(String.format("%02d", LocalDate.now().dayOfMonth)).setFontSize(fontSize).setBold()))
            datosSolicitudTable.addCell(Cell().add(Paragraph("FOLIO:").setFontSize(fontSize).setFontColor(whiteColor)).setBackgroundColor(headerColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(muestraData.folio).setFontSize(fontSize).setBold()))

            val datosClienteTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f))).useAllAvailableWidth()
            datosClienteTable.addCell(Cell().add(Paragraph("NOMBRE:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosClienteTable.addCell(Cell(1, 3).add(Paragraph(muestraData.clientePdm?.nombre_empresa ?: "N/A").setFontSize(fontSize).setBold()))
            datosClienteTable.addCell(Cell().add(Paragraph("DIRECCIÓN:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosClienteTable.addCell(Cell(1, 3).add(Paragraph(muestraData.clientePdm?.direccion ?: "").setFontSize(fontSize).setBold()))
            datosClienteTable.addCell(Cell().add(Paragraph("ATENCIÓN A:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosClienteTable.addCell(Cell().add(Paragraph(muestraData.clientePdm?.atencion ?: "").setFontSize(fontSize).setBold()))
            datosClienteTable.addCell(Cell().add(Paragraph("TELÉFONO:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosClienteTable.addCell(Cell().add(Paragraph(muestraData.clientePdm?.telefono ?: "").setFontSize(fontSize).setBold()))
            datosClienteTable.addCell(Cell().add(Paragraph("PUESTO:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosClienteTable.addCell(Cell().add(Paragraph(muestraData.clientePdm?.puesto ?: "").setFontSize(fontSize).setBold()))
            datosClienteTable.addCell(Cell().add(Paragraph("CORREO:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosClienteTable.addCell(Cell(1, 3).add(Paragraph(muestraData.clientePdm?.correo ?: "").setFontSize(fontSize).setBold()))

            tableEncabezado.addCell(Cell(1, 1).add(Paragraph("DATOS DE SOLICITUD").setFontColor(whiteColor)).setBackgroundColor(headerColor).setFontSize(fontSize))
            tableEncabezado.addCell(Cell(1, 2).add(Paragraph("DATOS DE QUIEN SOLICITA LOS ANÁLISIS").setFontColor(whiteColor)).setBackgroundColor(headerColor).setFontSize(fontSize))
            tableEncabezado.addCell(Cell().add(datosSolicitudTable)).setBorder(Border.NO_BORDER)
            tableEncabezado.addCell(Cell(1, 2).add(datosClienteTable)).setBorder(Border.NO_BORDER)

            mainTable.addCell(Cell().add(tableEncabezado).setBorder(Border.NO_BORDER))
            mainTable.addCell(Cell().add(qrImage).setVerticalAlignment(VerticalAlignment.MIDDLE).setHorizontalAlignment(HorizontalAlignment.RIGHT).setBorder(Border.NO_BORDER))
            mainTable.addCell(Cell().add(image).setVerticalAlignment(VerticalAlignment.MIDDLE).setHorizontalAlignment(HorizontalAlignment.RIGHT).setBorder(Border.NO_BORDER))
            document.add(mainTable)

            // Tabla de muestras
            val table = Table(floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)).setMarginTop(10f).setWidth(UnitValue.createPercentValue(100f))
            table.addHeaderCell(Cell(1, 7).add(Paragraph("Datos de las muestras colectadas").setFontColor(whiteColor).setFontSize(10f))
                .setBackgroundColor(headerColor).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE).setBorder(Border.NO_BORDER))
            table.addHeaderCell(Cell(1, 2).add(Paragraph("Estudios a realizar").setFontColor(whiteColor).setFontSize(10f))
                .setBackgroundColor(DeviceRgb(22, 127, 165)).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE).setBorder(Border.NO_BORDER))
            table.addHeaderCell(Cell(1, 1).add(Paragraph("").setFontColor(whiteColor).setFontSize(15f))
                .setBackgroundColor(headerColor).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE).setBorder(Border.NO_BORDER))

            table.addHeaderCell(Cell().add(Paragraph("FECHA").setFontSize(7f)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            table.addHeaderCell(Cell().add(Paragraph("REGISTRO").setFontSize(7f)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            table.addHeaderCell(Cell().add(Paragraph("NOMBRE").setFontSize(7f)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            table.addHeaderCell(Cell().add(Paragraph("ID LAB").setFontSize(7f)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            table.addHeaderCell(Cell().add(Paragraph("CANT").setFontSize(7f)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            table.addHeaderCell(Cell().add(Paragraph("TEMP").setFontSize(7f)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            table.addHeaderCell(Cell().add(Paragraph("LUGAR").setFontSize(7f)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            table.addHeaderCell(Cell().add(Paragraph("E MICRO").setFontSize(7f)).setBackgroundColor(DeviceRgb(22, 127, 165)).setFontColor(whiteColor))
            table.addHeaderCell(Cell().add(Paragraph("E FISICO").setFontSize(7f)).setBackgroundColor(DeviceRgb(22, 127, 165)).setFontColor(whiteColor))
            table.addHeaderCell(Cell().add(Paragraph("OBS").setFontSize(7f)).setBackgroundColor(headerColor).setFontColor(whiteColor))

            for (muestra in muestraData.muestras) {
                table.addCell(Cell().add(Paragraph(muestra.fechaMuestra ?: "").setFontSize(6f)))
                table.addCell(Cell().add(Paragraph(muestra.registroMuestra ?: "").setFontSize(6f)))
                table.addCell(Cell().add(Paragraph(muestra.nombreMuestra ?: "").setFontSize(6f)))
                table.addCell(Cell().add(Paragraph(muestra.idLab ?: "").setFontSize(6f)))
                table.addCell(Cell().add(Paragraph(muestra.cantidadAprox ?: "").setFontSize(6f)))
                table.addCell(Cell().add(Paragraph(muestra.tempM ?: "").setFontSize(6f)))
                table.addCell(Cell().add(Paragraph(muestra.lugarToma ?: "").setFontSize(6f)))
                table.addCell(Cell().add(Paragraph(muestra.emicro ?: "").setFontSize(6f)))
                table.addCell(Cell().add(Paragraph(muestra.efisico ?: "").setFontSize(6f)))
                table.addCell(Cell().add(Paragraph(muestra.observaciones ?: "").setFontSize(6f)))
            }
            document.add(table)

            // ====== FIRMAS ======
            // Convertir firmas a imágenes
            val streamAutoriza = ByteArrayOutputStream()
            datosFirma.firmaAutoriza?.compress(Bitmap.CompressFormat.PNG, 100, streamAutoriza)
            val imagedataAutoriza = ImageDataFactory.create(streamAutoriza.toByteArray())
            val signatureImageUno = Image(imagedataAutoriza).scaleToFit(60f, 40f)

            val streamMuestreador = ByteArrayOutputStream()
            datosFirma.firmaMuestreador?.compress(Bitmap.CompressFormat.PNG, 100, streamMuestreador)
            val imagedataMuestreador = ImageDataFactory.create(streamMuestreador.toByteArray())
            val signatureImageDos = Image(imagedataMuestreador).scaleToFit(60f, 40f)

            // Tabla de firmas (4 columnas)
            val firmaTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f)))
            firmaTable.setWidth(UnitValue.createPercentValue(100f)).setHorizontalAlignment(HorizontalAlignment.CENTER)
            firmaTable.setMarginRight(10f)

            // Encabezado QUIEN AUTORIZA
            val headerAutorizaCell = Cell(1, 3)
                .add(Paragraph("QUIÉN AUTORIZA ANÁLISIS DE LAS MUESTRAS")
                    .setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
                .setPadding(2f)
            val headerFirmaCell1 = Cell()
                .add(Paragraph("FIRMA").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(2f)

            firmaTable.addCell(headerAutorizaCell)
            firmaTable.addCell(headerFirmaCell1)

            // Datos de quien autoriza - NOMBRE
            val autorizaNombreCell = Cell()
                .add(Paragraph("NOMBRE:").setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setFontColor(whiteColor)
                .setPadding(2f)
            val autorizaNombreValueCell = Cell(1, 2)
                .add(Paragraph(datosFirma.nombreAutoAnalisis).setFontSize(fontSize).setBold())
                .setPadding(2f)

            firmaTable.addCell(autorizaNombreCell)
            firmaTable.addCell(autorizaNombreValueCell)
            firmaTable.addCell(Cell(2, 1).add(signatureImageUno).setPadding(2f))

            // Datos de quien autoriza - PUESTO
            val autorizaPuestoCell = Cell()
                .add(Paragraph("PUESTO:").setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setFontColor(whiteColor)
                .setPadding(2f)
            val autorizaPuestoValueCell = Cell(1, 2)
                .add(Paragraph(datosFirma.puestoAutoAnalisis).setFontSize(fontSize).setBold())
                .setPadding(2f)

            firmaTable.addCell(autorizaPuestoCell)
            firmaTable.addCell(autorizaPuestoValueCell)

            // Encabezado QUIEN TOMA LAS MUESTRAS
            val headerTomaCell = Cell(1, 3)
                .add(Paragraph("QUIÉN TOMA LAS MUESTRAS")
                    .setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
                .setPadding(2f)
            val headerFirmaCell2 = Cell()
                .add(Paragraph("FIRMA").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(2f)

            firmaTable.addCell(headerTomaCell)
            firmaTable.addCell(headerFirmaCell2)

            // Datos de quien toma - NOMBRE
            val tomaNombreCell = Cell()
                .add(Paragraph("NOMBRE:").setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setFontColor(whiteColor)
                .setPadding(2f)
            val tomaNombreValueCell = Cell(1, 2)
                .add(Paragraph(datosFirma.nombreMuestreador).setFontSize(fontSize).setBold())
                .setPadding(2f)

            firmaTable.addCell(tomaNombreCell)
            firmaTable.addCell(tomaNombreValueCell)
            firmaTable.addCell(Cell(2, 1).add(signatureImageDos).setPadding(2f))

            // Datos de quien toma - PUESTO
            val tomaPuestoCell = Cell()
                .add(Paragraph("PUESTO:").setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setFontColor(whiteColor)
                .setPadding(2f)
            val tomaPuestoValueCell = Cell(1, 2)
                .add(Paragraph(datosFirma.puestoMuestreador).setFontSize(fontSize).setBold())
                .setPadding(2f)

            firmaTable.addCell(tomaPuestoCell)
            firmaTable.addCell(tomaPuestoValueCell)

            // ====== TABLA PUNTO CRÍTICO ======
            var puntoCriticoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f)))
                .useAllAvailableWidth()

            val puntoCriticoHeader = Cell(1, 4)
                .add(Paragraph("PUNTO CRITICO PRE - ANALISIS").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            puntoCriticoTable.addCell(puntoCriticoHeader)

            // Subtabla de salida
            val subtablaSalida = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f)))
            subtablaSalida.setWidth(UnitValue.createPercentValue(100f))

            val subtablaSalidaFecha = Cell()
                .add(Paragraph("Fecha:").setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setFontColor(whiteColor)
            val subtablaSalidaTemp = Cell()
                .add(Paragraph("Temp:").setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setFontColor(whiteColor)
            val subtablaSalidaResp = Cell()
                .add(Paragraph("Resp:").setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setFontColor(whiteColor)

            subtablaSalida.addCell(subtablaSalidaFecha)
            subtablaSalida.addCell(Cell(1, 2).add(Paragraph(LocalDate.now().toString()).setFontSize(fontSize)))
            subtablaSalida.addCell(subtablaSalidaTemp)
            subtablaSalida.addCell(Cell(1, 2).add(Paragraph("").setFontSize(fontSize)))
            subtablaSalida.addCell(subtablaSalidaResp)
            subtablaSalida.addCell(Cell(1, 2).add(Paragraph(datosFirma.nombreMuestreador).setFontSize(fontSize)))
            subtablaSalida.addCell(Cell(1, 3).add(Paragraph().setFontSize(fontSize)))

            // Subtabla de entrada
            val subtablaEntrada = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f)))
            subtablaEntrada.setWidth(UnitValue.createPercentValue(100f))

            val subtablaEntradaFecha = Cell()
                .add(Paragraph("Fecha:").setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setFontColor(whiteColor)
            val subtablaEntradaTemp = Cell()
                .add(Paragraph("Temp:").setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setFontColor(whiteColor)
            val subtablaEntradaResp = Cell()
                .add(Paragraph("Resp:").setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setFontColor(whiteColor)

            subtablaEntrada.addCell(subtablaEntradaFecha)
            subtablaEntrada.addCell(Cell(1, 2).add(Paragraph("").setFontSize(fontSize)))
            subtablaEntrada.addCell(subtablaEntradaTemp)
            subtablaEntrada.addCell(Cell(1, 2).add(Paragraph("").setFontSize(fontSize)))
            subtablaEntrada.addCell(subtablaEntradaResp)
            subtablaEntrada.addCell(Cell(1, 2).add(Paragraph("").setFontSize(fontSize)))
            subtablaEntrada.addCell(Cell(1, 3).add(Paragraph("").setFontSize(fontSize)))

            val datosPuntoCriticaCell = Cell(2, 2)
                .add(Paragraph("DATOS DE SALIDA:").setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setFontColor(whiteColor)
                .setTextAlignment(TextAlignment.CENTER)
            val datosrecepcionPuntoCritico = Cell(2, 2)
                .add(Paragraph("DATOS DE RECEPCION:").setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setFontColor(whiteColor)
                .setTextAlignment(TextAlignment.CENTER)
            val idCliente = Cell(2, 2)
                .add(Paragraph("ID CLIENTE:").setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setFontColor(whiteColor)
                .setTextAlignment(TextAlignment.CENTER)

            puntoCriticoTable.addCell(datosPuntoCriticaCell)
            puntoCriticoTable.addCell(Cell(2, 2).add(subtablaSalida).setBorder(Border.NO_BORDER))
            puntoCriticoTable.addCell(datosrecepcionPuntoCritico)
            puntoCriticoTable.addCell(Cell(2, 2).add(subtablaEntrada).setBorder(Border.NO_BORDER))
            puntoCriticoTable.addCell(idCliente)
            puntoCriticoTable.addCell(Cell(2, 2).add(Paragraph("").setFontSize(fontSize)))

            // Tabla principal que combina firmas y punto crítico
            val tablaPrincipal = Table(2)
            tablaPrincipal.setWidth(UnitValue.createPercentValue(100f))
            tablaPrincipal.addCell(Cell().add(firmaTable).setBorder(Border.NO_BORDER))
            tablaPrincipal.addCell(Cell().add(puntoCriticoTable).setBorder(Border.NO_BORDER))

            document.add(tablaPrincipal)

            // ====== FOOTER ======
            val tableFooter = Table(UnitValue.createPercentArray(floatArrayOf(1f, 0.5f, 1.5f, 0.5f, 1.5f)))
            tableFooter.setWidth(UnitValue.createPercentValue(100f))
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setBackgroundColor(headerColor)
                .setBorder(Border.NO_BORDER)

            val fontSizeFooter = 6f
            tableFooter.addCell(Cell().add(Paragraph("recepcionlab.lesa@gmail.com")
                .setFontColor(whiteColor).setFontSize(fontSizeFooter)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("998 310 8622")
                .setFontColor(whiteColor).setFontSize(fontSizeFooter)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell(2, 2).add(Paragraph("DOCUMENTO CONTROLADO\n Documento propiedad de Centro Integral en Servicios de Laboratorio de Agua y Alimentos S.A de C.V.\n No puede reproducirse en forma parcial o total, si nla previa autorizacion del Laboratorio")
                .setFontColor(whiteColor).setFontSize(fontSizeFooter).setTextAlignment(TextAlignment.CENTER)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("F-ING-LAB-02/ VERSION 0")
                .setFontColor(whiteColor).setFontSize(fontSizeFooter).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com")
                .setFontColor(whiteColor).setFontSize(fontSizeFooter)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("998 310 8623")
                .setFontColor(whiteColor).setFontSize(fontSizeFooter)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("A.FRANCISCO I. MADERO MZ 107 LT 12 Int: LOCAL 4 REGION 94. CP 7717. Benito Juarez, Q.roo")
                .setFontColor(whiteColor).setFontSize(5f).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))

            document.add(tableFooter)

            document.close()
            Log.d("MainActivity2", "PDF generado: ${file.absolutePath}")

        } catch (e: Exception) {
            Log.e("MainActivity2", "Error al generar PDF para ${muestraData.folio}", e)
            throw e // Re-lanzar excepción para manejo en generarPDFsFirmados
        }
    }

    private fun crearQrImage(muestraData: MuestraData, pdfDoc: PdfDocument): Image {
        val folio = muestraData.folio
        val expires = (System.currentTimeMillis() / 1000L) + 86400
        val secret = "LesaaClaveSecreta20251256"
        val data = "$folio|$expires"
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        val secretKeySpec = javax.crypto.spec.SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)
        val tokenBytes = mac.doFinal(data.toByteArray())
        val token = tokenBytes.joinToString("") { "%02x".format(it) }
        val url = "https://grupolesaa.com.mx/scan?folio=$folio&expires=$expires&token=$token"
        val qrCode = BarcodeQRCode(url)
        val qrObject = qrCode.createFormXObject(null, pdfDoc)
        return Image(qrObject).scaleToFit(100f, 100f)
    }

    private fun mostrarOpcionesDeEnvio(
        datosFirma: DatosFirmaPlan,
        foliosSeleccionados: List<MuestraData>,
        exitosos: Int,
        fallidos: Int,
        resultados: List<Pair<String, Boolean>>
    ) {
        val mensaje = if (fallidos == 0) {
            "✓ PDFs generados exitosamente para $exitosos folio(s)"
        } else {
            "✓ $exitosos generados correctamente\n✗ $fallidos fallidos"
        }

        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()

        if (fallidos > 0) {
            // Mostrar diálogo con detalle de errores
            val fallidos_detalle = resultados.filter { !it.second }.joinToString("\n") { "• ${it.first}" }
            AlertDialog.Builder(this)
                .setTitle("Algunos PDFs no se generaron")
                .setMessage("Folios con error:\n$fallidos_detalle")
                .setPositiveButton("OK", null)
                .show()
        }

        // Preguntar si desea enviar correos
        if (exitosos > 0 && datosFirma.correo.isNotBlank()) {
            AlertDialog.Builder(this)
                .setTitle("Enviar correos")
                .setMessage("¿Deseas enviar los PDFs a:\n• ${datosFirma.correo}\n• recepcionlab.lesa@gmail.com\n• operacioneslab.lesa@gmail.com?")
                .setPositiveButton("Enviar") { _, _ ->
                    enviarCorreos(datosFirma, resultados)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun enviarCorreos(datosFirma: DatosFirmaPlan, resultados: List<Pair<String, Boolean>>) {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Enviando correos")
            .setMessage("Por favor espera...")
            .setCancelable(false)
            .show()

        val foliosExitosos = resultados.filter { it.second }.map { it.first }
        val documentoDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val correosDestino = listOf(
            datosFirma.correo,
            "recepcionlab.lesa@gmail.com",
            "operacioneslab.lesa@gmail.com"
        )

        coroutineJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                for (folio in foliosExitosos) {
                    val pdfFile = File(documentoDir, "Muestras-Folio-$folio.pdf")
                    if (pdfFile.exists()) {
                        // Enviar a cada correo
                        for (correo in correosDestino) {
                            enviarCorreoConPDF(correo, pdfFile, folio)
                        }
                    } else {
                        Log.w("MainActivity2", "Archivo PDF no encontrado para folio: $folio")
                    }
                }

                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@MainActivity2,
                        "Correos enviados exitosamente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Log.e("MainActivity2", "Error al enviar correos", e)
                    Toast.makeText(
                        this@MainActivity2,
                        "Error al enviar correos: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun enviarCorreoConPDF(emailAddress: String, file: File, folio: String) {
        val workData = Data.Builder()
            .putString("emailAddress", emailAddress)
            .putString("filePath", file.absolutePath)
            .putString("subject", "Registro de muestreo - Folio $folio")
            .putString("messageText", """
                <h1>Registro de Muestreo</h1>
                <p>Estimado cliente,</p>
                <p>Se adjunta el registlo de muestreo con folio <strong>$folio</strong>.</p>
                <p>Agradecemos su confianza.</p>
                <br>
                <p><strong>Laboratorio LESA</strong></p>
            """.trimIndent())
            .build()

        val emailWorkRequest = OneTimeWorkRequestBuilder<SendEmailWorker>()
            .setInputData(workData)
            .build()

        WorkManager.getInstance(this).enqueue(emailWorkRequest)
        Log.d("MainActivity2", "Tarea de envío de correo encolada para: $emailAddress")
    }
}

