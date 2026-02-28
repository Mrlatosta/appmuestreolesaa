package com.example.aplicacionlesaa

import SendEmailWorker
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
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDate

class FirmaFinalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFirmaFinalBinding
    private val pdmMap: MutableMap<String, MutableList<MuestraData>> = mutableMapOf()
    private val planesHoy: MutableList<String> = mutableListOf()
    private var selectedPlan: String? = null
    private lateinit var foliosAdapter: FoliosAdapter
    private lateinit var muestraAdapter: MuestraResumenAdapter

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
                val json = archivo.bufferedReader().use { it.readText() }
                val gson = Gson()
                val muestraData = gson.fromJson(json, MuestraData::class.java)
                val pdm = muestraData.planMuestreo ?: "Sin PDM"
                pdmMap.getOrPut(pdm) { mutableListOf() }.add(muestraData)
            } catch (e: Exception) {
                Log.e("FirmaFinalActivity", "Error leyendo archivo: ${archivo.name}", e)
            }
        }

        planesHoy.addAll(pdmMap.keys.sorted())
        cargarTablaPlanes()
    }

    private fun cargarTablaPlanes() {
        val table = binding.tablePlanes
        // Eliminar todas las filas excepto el encabezado
        while (table.childCount > 1) { table.removeViewAt(1) }

        for (pdm in planesHoy) {
            val folios = pdmMap[pdm] ?: continue
            val cliente = folios.firstOrNull()?.clientePdm?.nombre_empresa ?: "-"
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
        val folios = pdmMap[pdm] ?: emptyList()
        binding.tvPlanSeleccionado.text = "Plan seleccionado: $pdm"
        foliosAdapter.updateData(folios)
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

                generarPdfsConEstructuraCompleta(this, foliosSeleccionados, datosFirma)
                mostrarOpcionesDeEnvio(datosFirma, foliosSeleccionados)
                dialog2.dismiss()
            }

            dialogMuestreador.setNegativeButton("Cancelar") { dialog2, _ -> dialog2.dismiss() }
            dialogMuestreador.create().show()
        }

        dialogAutoriza.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        dialogAutoriza.create().show()
    }

    private fun generarPdfsConEstructuraCompleta(
        context: Context,
        foliosSeleccionados: List<MuestraData>,
        datosFirma: DatosFirmaPlan
    ) {
        foliosSeleccionados.forEach { muestraData ->
            generarPdfPlan(context, muestraData, datosFirma)
        }
    }

    private fun generarPdfPlan(context: Context, muestraData: MuestraData, datosFirma: DatosFirmaPlan) {
        val pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val file = File(pdfPath, "Muestras-Folio-${muestraData.folio}.pdf")

        try {
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)

            // Crear QR Code
            val qrImage = crearQrImage(muestraData, pdfDocument)

            val document = Document(pdfDocument, PageSize.A4.rotate())

            // Agregar event handler para paginación
            val footerHandler = FooterEventHandler(document)
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler)

            // Cargar logo desde recursos
            val inputStream = context.resources.openRawResource(R.raw.logorectangulartrans)
            val byteArrayOutputStream = ByteArrayOutputStream()
            var nextByte = inputStream.read()
            while (nextByte != -1) {
                byteArrayOutputStream.write(nextByte)
                nextByte = inputStream.read()
            }
            val imageData = byteArrayOutputStream.toByteArray()
            val image = Image(ImageDataFactory.create(imageData))
            image.scaleToFit(125f, 90f)

            // Definir colores
            val headerColor = DeviceCmyk(99, 38, 0, 67)
            val subHeaderColor = DeviceRgb(153, 204, 255)
            val blueColor = DeviceRgb(22, 127, 165)
            val whiteColor = DeviceRgb(255, 255, 255)
            val fontSize = 8f

            // ====== TABLA PRINCIPAL (3 columnas) ======
            val mainTable = Table(UnitValue.createPercentArray(floatArrayOf(4f, 1f, 1f)))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)

            // ====== TABLA DE ENCABEZADO ======
            val tableEncabezado = Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f)))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)

            // Encabezado principal
            val mainHeaderCell = Cell(1, 2)
                .add(Paragraph("F-ING-LAB-02. HOJA DE REGISTRO DE CAMPO")
                    .setFontColor(whiteColor)
                    .setFontSize(fontSize)
                    .setBold())
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            tableEncabezado.addCell(mainHeaderCell)

            // Sub-encabezado
            val subHeaderCell = Cell(1, 2)
                .add(Paragraph("Servicios que generan valor")
                    .setFontColor(whiteColor)
                    .setFontSize(fontSize)
                    .setBold())
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            tableEncabezado.addCell(subHeaderCell)

            // ====== TABLA DE DATOS DE SOLICITUD ======
            val datosSolicitudTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
            datosSolicitudTable.setWidth(UnitValue.createPercentValue(100f))
            datosSolicitudTable.setHeight(UnitValue.createPercentValue(100f))

            datosSolicitudTable.addCell(Cell().add(Paragraph("AÑO:").setFontSize(fontSize))
                .setFontColor(whiteColor).setBackgroundColor(headerColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(LocalDate.now().year.toString())
                .setFontSize(fontSize).setBold()).setBackgroundColor(whiteColor))

            datosSolicitudTable.addCell(Cell().add(Paragraph("MES:").setFontSize(fontSize))
                .setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(String.format("%02d", LocalDate.now().monthValue))
                .setFontSize(fontSize).setBold()).setBackgroundColor(whiteColor))

            datosSolicitudTable.addCell(Cell().add(Paragraph("DÍA:").setFontSize(fontSize))
                .setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(String.format("%02d", LocalDate.now().dayOfMonth))
                .setFontSize(fontSize).setBold()).setBackgroundColor(whiteColor))

            datosSolicitudTable.addCell(Cell().add(Paragraph("FOLIO:").setFontSize(fontSize)
                .setFontColor(whiteColor)).setBackgroundColor(headerColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(muestraData.folio)
                .setFontSize(fontSize).setBold()).setBackgroundColor(whiteColor))

            // ====== TABLA DE DATOS DE QUIEN SOLICITA ======
            val datosSolicitanteTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f)))
                .useAllAvailableWidth()

            datosSolicitanteTable.addCell(Cell().add(Paragraph("NOMBRE:").setFontSize(fontSize))
                .setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell(1, 3).add(Paragraph(muestraData.clientePdm?.nombre_empresa ?: "")
                .setFontSize(fontSize).setBold()))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("DIRECCIÓN:").setFontSize(fontSize))
                .setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell(1, 3).add(Paragraph(muestraData.clientePdm?.direccion ?: "")
                .setFontSize(fontSize).setBold()))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("ATENCIÓN A:").setFontSize(fontSize))
                .setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell().add(Paragraph(muestraData.clientePdm?.atencion ?: "")
                .setFontSize(fontSize).setBold()))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("TELÉFONO:").setFontSize(fontSize))
                .setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell().add(Paragraph(muestraData.clientePdm?.telefono ?: "")
                .setFontSize(fontSize).setBold()))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("PUESTO:").setFontSize(fontSize))
                .setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell().add(Paragraph(muestraData.clientePdm?.puesto ?: "")
                .setFontSize(fontSize).setBold()))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("CORREO:").setFontSize(fontSize))
                .setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell(1, 3).add(Paragraph(muestraData.clientePdm?.correo ?: "")
                .setFontSize(fontSize).setBold()))

            // Agregar sub-tablas al encabezado
            tableEncabezado.addCell(Cell(1, 1).add(Paragraph("DATOS DE SOLICITUD").setFontColor(whiteColor))
                .setBackgroundColor(headerColor).setFontSize(fontSize))
            tableEncabezado.addCell(Cell(1, 2).add(Paragraph("DATOS DE QUIEN SOLICITA LOS ANÁLISIS").setFontColor(whiteColor))
                .setBackgroundColor(headerColor).setFontSize(fontSize))

            tableEncabezado.addCell(Cell().add(datosSolicitudTable).setBorder(Border.NO_BORDER))
            tableEncabezado.addCell(Cell(1, 2).add(datosSolicitanteTable).setBorder(Border.NO_BORDER))

            // Agregar tabla de encabezado, QR y logo a la tabla principal
            mainTable.addCell(Cell().add(tableEncabezado).setBorder(Border.NO_BORDER))
            mainTable.addCell(Cell().add(qrImage)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setBorder(Border.NO_BORDER))
            mainTable.addCell(Cell().add(image)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setBorder(Border.NO_BORDER))

            document.add(mainTable)

            // ====== TABLA DE MUESTRAS ======
            val table = Table(floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f))
                .setMarginTop(10f)
            table.setWidth(UnitValue.createPercentValue(100f))

            // Encabezados de datos de las muestras
            val tabeadercell = Cell(1, 7)
                .add(Paragraph("Datos de las muestras colectadas").setFontColor(whiteColor).setFontSize(10f))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            table.addHeaderCell(tabeadercell)

            val estutablcell = Cell(1, 2)
                .add(Paragraph("Estudios a realizar").setFontColor(whiteColor).setFontSize(10f))
                .setBackgroundColor(blueColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            table.addHeaderCell(estutablcell)

            val cellobsv = Cell(1, 1)
                .add(Paragraph("").setFontColor(whiteColor).setFontSize(15f))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            table.addHeaderCell(cellobsv)

            // Agregar encabezados de columnas con estilo de MainActivity2
            val headers = arrayOf(
                "NO.", "Registro", "Nombre de Muestra",
                "Lugar de Toma.", "Descripción", "Cantidad Aprox.",
                "TEMP.°[C]", "MB", "FQ", "Observaciones"
            )

            val columnWidths = mapOf(
                "NO." to 10f,
                "Registro" to 50f,
                "Nombre de Muestra" to 120f,
                "Lugar de Toma." to 150f,
                "Descripción" to 160f,
                "Cantidad Aprox." to 80f,
                "TEMP.°[C]" to 50f,
                "MB" to 50f,
                "FQ" to 50f,
                "Observaciones" to 120f
            )

            headers.forEach { header ->
                val bgColor = if (header == "MB" || header == "FQ") DeviceRgb(22, 127, 165) else DeviceCmyk(99, 38, 0, 67)
                val headerCell = Cell().add(Paragraph(header))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBackgroundColor(bgColor)
                    .setFontColor(DeviceRgb(255, 255, 255))
                    .setWidth(columnWidths[header] ?: 100f)
                table.addHeaderCell(headerCell)
            }

            // Agregar filas de datos
            var idx = 1
            for (muestra in muestraData.muestras) {
                table.addCell(Cell().add(Paragraph(idx.toString()).setFontSize(7f)))
                table.addCell(Cell().add(Paragraph(muestra.registroMuestra ?: "").setFontSize(7f)))
                table.addCell(Cell().add(Paragraph(muestra.nombreMuestra ?: "").setFontSize(7f)))
                table.addCell(Cell().add(Paragraph(muestra.lugarToma ?: "").setFontSize(7f)))
                table.addCell(Cell().add(Paragraph(muestra.descripcionM ?: "").setFontSize(7f)))
                table.addCell(Cell().add(Paragraph(muestra.cantidadAprox ?: "").setFontSize(7f)))
                table.addCell(Cell().add(Paragraph(muestra.tempM ?: "").setFontSize(7f)))
                table.addCell(Cell().add(Paragraph(muestra.emicro ?: "").setFontSize(7f)))
                table.addCell(Cell().add(Paragraph(muestra.efisico ?: "").setFontSize(7f)))
                table.addCell(Cell().add(Paragraph(muestra.observaciones ?: "").setFontSize(7f)))
                idx++
            }

            table.setFontSize(7f)
            document.add(table)

            // Nomenclatura
            val paragrafoNomenclaturas = Paragraph("MA=Mesofilicos Aerobios - CT=Coliformes Totales - MH=Mohos - LV=Levaduras - CF=Coliformes Fecales - EC=Escherichia Coli - SA=Staphylococcus Aureus - SS=Salmonella spp - LM=Listeria Monocytogenes Vc=Vibrio Cholerae spp. - VP=Vibrio Parahemolitico spp. - PS=Pseudomona - CP = Clostridium Perfringens = BC=Bacillus Cereus - LP=Legionella spp. -ACA= Acanthamoeba spp. - NAE=Naegleria spp. - EFC=Enterococcus Fecales. - GL=Giardia Lamblia. - CLL=Cloro libre. - CCT= Cloro total.-PH=Potencial de Hidrógeno.-Crnas=Cloraminas,DUR=Dureza.- Alk=Alcalinidad. - SDT=Sólidos Disueltos Totales. - CE-Conductividad Electrica. - TUR=Turbidez CyA")
                .setFontColor(DeviceRgb(1, 1, 1))
                .setFontSize(5f)
            document.add(paragrafoNomenclaturas)

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

            // Tabla de firmas (4 columnas como en MainActivity2)
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

            Log.d("FirmaFinalActivity", "PDF generado: ${file.absolutePath}")
            Toast.makeText(context, "PDF generado: ${file.name}", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("FirmaFinalActivity", "Error al generar PDF", e)
            Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    class FooterEventHandler(private val document: Document) : IEventHandler {
        override fun handleEvent(event: Event) {
            val pdfEvent = event as PdfDocumentEvent
            val pdfDoc = pdfEvent.document
            val page = pdfEvent.page

            val x = 34f
            val y = 20f
            val width = UnitValue.createPercentValue(105f)
            val height = 100f

            var paragraph: Paragraph
            if (pdfDoc.getPageNumber(page) == pdfDoc.numberOfPages) {
                paragraph = Paragraph("Pagina: ${pdfDoc.getPageNumber(page)} de ${pdfDoc.numberOfPages}")
            } else {
                paragraph = Paragraph("Pagina: ${pdfDoc.getPageNumber(page)} de ${pdfDoc.numberOfPages}")
                paragraph.setFixedPosition(pdfDoc.getPageNumber(page), 25f, y, width)
            }
            document.add(paragraph)
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
        Log.i("URL_QR", url)
        val qrCode = BarcodeQRCode(url)
        val qrObject = qrCode.createFormXObject(null, pdfDoc)
        return Image(qrObject).scaleToFit(100f, 100f)
    }

    private fun mostrarOpcionesDeEnvio(datosFirma: DatosFirmaPlan, foliosSeleccionados: List<MuestraData>) {
        val correosDestino = listOf(
            datosFirma.correo,
            "recepcionlab.lesa@gmail.com",
            "operacioneslab.lesa@gmail.com"
        )

        foliosSeleccionados.forEach { muestraData ->
            val pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val file = File(pdfPath, "Muestras-Folio-${muestraData.folio}.pdf")
            val subject = "Resultados de muestreo - Folio ${muestraData.folio}"
            
            val messageText = """
                <h2>Registro de Muestreo</h2>
                <p><strong>Folio:</strong> ${muestraData.folio}</p>
                <p>Adjunto encontrará el registro de muestreo solicitado.</p>
                <p>Agradecemos su confianza.</p>
                <br>
                <p><strong>Laboratorio LESA</strong></p>
            """.trimIndent()

            // Enviar a cada correo
            correosDestino.forEach { correo ->
                val data = Data.Builder()
                    .putString("emailAddress", correo)
                    .putString("filePath", file.absolutePath)
                    .putString("subject", subject)
                    .putString("messageText", messageText)
                    .build()
                val sendEmailRequest = OneTimeWorkRequestBuilder<SendEmailWorker>()
                    .setInputData(data)
                    .build()
                WorkManager.getInstance(this).enqueue(sendEmailRequest)
            }
        }
        Toast.makeText(this, "PDFs generados y correos enviados para ${foliosSeleccionados.size} folio(s)", Toast.LENGTH_LONG).show()
    }
}
