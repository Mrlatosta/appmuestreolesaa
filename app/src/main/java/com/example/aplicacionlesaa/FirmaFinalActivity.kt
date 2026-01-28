package com.example.aplicacionlesaa

import SendEmailWorker
import android.content.Context
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aplicacionlesaa.adapter.FoliosAdapter
import com.example.aplicacionlesaa.adapter.MuestraResumenAdapter
import com.example.aplicacionlesaa.databinding.ActivityFirmaFinalBinding
import com.example.aplicacionlesaa.databinding.ActivityMainBinding
import com.example.aplicacionlesaa.model.DatosFirmaPlan
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.io.source.ByteArrayOutputStream
import com.itextpdf.kernel.colors.DeviceCmyk
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.events.Event
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph

import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import com.example.aplicacionlesaa.model.MuestraData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itextpdf.barcodes.BarcodeQRCode
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.element.Image
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDate

class FirmaFinalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFirmaFinalBinding

    // ===== DATA =====
    private val pdmMap: MutableMap<String, MutableList<MuestraData>> = mutableMapOf()
    private val planesHoy: MutableList<String> = mutableListOf()

    private var selectedPlan: String? = null
    private var foliosSeleccionados: List<MuestraData> = emptyList()

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

        binding.recyclerMuestras.layoutManager = LinearLayoutManager(this)
        muestraAdapter = MuestraResumenAdapter(emptyList())
        binding.recyclerMuestras.adapter = muestraAdapter

        cargarArchivosHoyAgrupados()

        binding.btnFirmarPlan.setOnClickListener {
            if (foliosSeleccionados.isEmpty()) {
                Toast.makeText(this, "Selecciona un plan de muestreo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mostrarDialogoFirma()
        }
    }

    // =====================================================
    // =============== CARGA JSONs =========================
    // =====================================================

    private fun cargarArchivosHoyAgrupados() {
        val directorio =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

        if (!directorio.exists()) return

        val fechaHoy = LocalDate.now().toString()

        val archivosHoy = directorio.listFiles { file ->
            file.extension == "json" && file.name.contains(fechaHoy)
        }

        archivosHoy?.forEach { archivo ->
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
        }

        cargarTablaPlanes()
    }

    // =====================================================
    // ================= TABLA PLANES ======================
    // =====================================================

    private fun cargarTablaPlanes() {
        val table = binding.tablePlanes
        table.removeViews(1, table.childCount - 1)

        for (pdm in planesHoy) {
            val folios = pdmMap[pdm] ?: continue
            val cliente = folios.first().clientePdm?.nombre_empresa ?: "-"

            val row = TableRow(this).apply {
                setOnClickListener {
                    mostrarFoliosDelPlan(pdm)
                }
            }

            row.addView(createCell(pdm))
            row.addView(createCell(cliente))
            row.addView(createCell(folios.size.toString()))
            row.addView(createInfoCell(pdm))

            table.addView(row)
        }
    }

    private fun createCell(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setPadding(16, 16, 16, 16)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun createInfoCell(pdm: String): View =
        ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_view)
            setPadding(16, 16, 16, 16)
            setColorFilter(Color.parseColor("#4CAF50"))
            setOnClickListener {
                mostrarFoliosDelPlan(pdm)
            }
        }

    // =====================================================
    // =============== FOLIOS POR PLAN =====================
    // =====================================================

    private fun mostrarFoliosDelPlan(pdm: String) {
        selectedPlan = pdm
        foliosSeleccionados = pdmMap[pdm] ?: emptyList()

        binding.tvPlanSeleccionado.text = "Plan seleccionado: $pdm"

        binding.recyclerFolios.layoutManager = LinearLayoutManager(this)
        binding.recyclerFolios.adapter =
            FoliosAdapter(foliosSeleccionados) { muestraData ->
                muestraAdapter.updateData(muestraData.muestras)
            }
    }

    // =====================================================
    // ================== FIRMA ============================
    // =====================================================

    private fun mostrarDialogoFirma() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_firma_plan, null)

        val txtNombreAuto = dialogView.findViewById<EditText>(R.id.txtNombreAutoAnalisis)
        val txtPuestoAuto = dialogView.findViewById<EditText>(R.id.txtPuestoAutoAnalisis)
        val txtNombreMuestreador = dialogView.findViewById<EditText>(R.id.txtNombreMuestreador)
        val txtPuestoMuestreador = dialogView.findViewById<EditText>(R.id.txtPuestoMuestreador)
        val txtCorreo = dialogView.findViewById<EditText>(R.id.txtCorreo)

        val signatureAutoriza = dialogView.findViewById<SignatureView>(R.id.signatureAutoriza)
        val signatureMuestreador = dialogView.findViewById<SignatureView>(R.id.signatureMuestreador)

        AlertDialog.Builder(this)
            .setTitle("Firmar plan de muestreo")
            .setView(dialogView)
            .setPositiveButton("Firmar y generar") { _, _ ->

                val datosFirma = DatosFirmaPlan(
                    nombreAutoAnalisis = txtNombreAuto.text.toString(),
                    puestoAutoAnalisis = txtPuestoAuto.text.toString(),
                    nombreMuestreador = txtNombreMuestreador.text.toString(),
                    puestoMuestreador = txtPuestoMuestreador.text.toString(),
                    correo = txtCorreo.text.toString(),
                    firmaAutoriza = signatureAutoriza.getSignatureBitmap(),
                    firmaMuestreador = signatureMuestreador.getSignatureBitmap()
                )

                generarPDFsFirmados(datosFirma)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun saveDataToJson(context: Context, muestraData: MuestraData, filename: String) {
        val gson = Gson()
        val jsonString = gson.toJson(muestraData)

        // Obtener la ruta de la carpeta Documents
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            .toString()

        // Crear el archivo en la carpeta Documents
        val file = File(documentsDir, filename)

        // Escribir el archivo
        file.writeText(jsonString)
    }

    fun crearQrImage(muestraData: MuestraData, pdfDoc: PdfDocument): Image {
        val folio = muestraData.folio
        val expires = (System.currentTimeMillis() / 1000L) + 86400 // expira en 24h
        val secret = "LesaaClaveSecreta20251256"  // puedes mandarlo desde backend seguro

        // Generar el token con HMAC SHA256
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
            }else{
                paragraph = Paragraph("Pagina: ${pdfDoc.getPageNumber(page)} de ${pdfDoc.numberOfPages}")
                paragraph.setFixedPosition(pdfDoc.getPageNumber(page), 25f, y, width)
            }
            document.add(paragraph)


            // Añadir el footer al documento



        }
    }

    private fun addTableHeader(table: Table) {
        val headers = arrayOf(
            "NO.", "Registro", "Nombre de Muestra",
            "Lugar de Toma.", "Descripción", "Cantidad Aprox.",
            "TEMP.°[C]", "MB", "FQ", "Observaciones"
        )

        val columnWidths = mapOf(
            "NO." to 10f,
//            "Fecha de Muestra" to 50f,
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
            val bgColor = if (header == "MB" || header == "FQ") DeviceRgb(22, 127, 165) else DeviceCmyk(99, 38, 0, 67	)
            val headerCell = Cell().add(Paragraph(header))
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(bgColor)
                .setFontColor(DeviceRgb(255, 255, 255))
                .setWidth(columnWidths[header] ?: 100f) // Valor por defecto si no está en el mapa

            table.addHeaderCell(headerCell)
        }
    }


    private fun addTableRow(table: Table, muestra: Muestra) {
        table.addCell(muestra.numeroMuestra)
//        table.addCell(muestra.fechaMuestra)
        table.addCell(muestra.registroMuestra)
        table.addCell(muestra.nombreMuestra)
//        table.addCell(muestra.idLab)
        table.addCell(muestra.lugarToma)
        table.addCell(muestra.descripcionM)
        table.addCell(muestra.cantidadAprox)
        table.addCell(muestra.tempM)
        table.addCell(muestra.emicro)
        table.addCell(muestra.efisico)
        table.addCell(muestra.observaciones)
    }

    private fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }


    private fun savePdfPlan(
        context: Context,
        muestraData: MuestraData,
        datosFirma: DatosFirmaPlan
    ) {

        val pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val file = File(pdfPath, "Muestras-Folio-${muestraData.folio}.pdf")

        try {
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)

            val qrImage = crearQrImage(muestraData, pdfDocument)

            val document = Document(pdfDocument, PageSize.A4.rotate())
            val footerHandler = FooterEventHandler(document)
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler)

            // ================= LOGO =================
            val imageBytes = context.resources
                .openRawResource(R.raw.logorectangulartrans)
                .readBytes()

            val image = Image(ImageDataFactory.create(imageBytes))
                .scaleToFit(125f, 90f)

            // ================= COLORES =================
            val headerColor = DeviceCmyk(99, 38, 0, 67)
            val whiteColor = DeviceRgb(255, 255, 255)
            val fontSize = 8f

            // ================= TABLA PRINCIPAL =================
            val mainTable = Table(UnitValue.createPercentArray(floatArrayOf(4f, 1f, 1f)))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)

            val tableEncabezado = Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f)))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)

            tableEncabezado.addCell(
                Cell(1, 2)
                    .add(
                        Paragraph("F-ING-LAB-02. HOJA DE REGISTRO DE CAMPO")
                            .setFontColor(whiteColor)
                            .setFontSize(fontSize)
                            .setBold()
                    )
                    .setBackgroundColor(headerColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(Border.NO_BORDER)
            )

            tableEncabezado.addCell(
                Cell(1, 2)
                    .add(
                        Paragraph("Servicios que generan valor")
                            .setFontColor(whiteColor)
                            .setFontSize(fontSize)
                            .setBold()
                    )
                    .setBackgroundColor(headerColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(Border.NO_BORDER)
            )

            // ================= DATOS SOLICITUD =================
            val datosSolicitudTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
            datosSolicitudTable.addCell(
                Cell().add(Paragraph("FOLIO:").setFontSize(fontSize))
                    .setBackgroundColor(headerColor)
                    .setFontColor(whiteColor)
            )
            datosSolicitudTable.addCell(
                Cell().add(Paragraph(muestraData.folio).setFontSize(fontSize).setBold())
            )

            // ================= DATOS CLIENTE =================
            val datosSolicitanteTable =
                Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f)))

            datosSolicitanteTable.addCell(
                Cell().add(Paragraph("CLIENTE:").setFontSize(fontSize))
                    .setBackgroundColor(headerColor)
                    .setFontColor(whiteColor)
            )
            datosSolicitanteTable.addCell(
                Cell().add(
                    Paragraph(muestraData.clientePdm?.nombre_empresa ?: "")
                        .setFontSize(fontSize)
                        .setBold()
                )
            )

            tableEncabezado.addCell(datosSolicitudTable)
            tableEncabezado.addCell(datosSolicitanteTable)

            mainTable.addCell(
                Cell().add(tableEncabezado).setBorder(Border.NO_BORDER)
            )
            mainTable.addCell(
                Cell().add(qrImage).setBorder(Border.NO_BORDER)
            )
            mainTable.addCell(
                Cell().add(image).setBorder(Border.NO_BORDER)
            )

            document.add(mainTable)

            // ================= TABLA MUESTRAS =================
            val table = Table(floatArrayOf(1f, 1f, 2f, 2f, 2f, 1f, 1f, 1f, 1f, 2f))
                .useAllAvailableWidth()
                .setMarginTop(10f)

            addTableHeader(table)

            muestraData.muestras.forEach {
                addTableRow(table, it)
            }

            table.setFontSize(7f)
            document.add(table)

            // ================= FIRMAS =================
            val firmaAutorizaImg = Image(
                ImageDataFactory.create(bitmapToBytes(datosFirma.firmaAutoriza))
            ).scaleToFit(100f, 100f)

            val firmaMuestreadorImg = Image(
                ImageDataFactory.create(bitmapToBytes(datosFirma.firmaMuestreador))
            ).scaleToFit(100f, 100f)

            val firmaTable =
                Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f)))
                    .useAllAvailableWidth()

            firmaTable.addCell(
                Cell(1, 3)
                    .add(
                        Paragraph("QUIÉN AUTORIZA")
                            .setFontSize(fontSize)
                            .setFontColor(whiteColor)
                    )
                    .setBackgroundColor(headerColor)
            )
            firmaTable.addCell(
                Cell().add(Paragraph("FIRMA")).setBackgroundColor(headerColor)
                    .setFontColor(whiteColor)
            )

            firmaTable.addCell(
                Cell().add(
                    Paragraph("NOMBRE: ${datosFirma.nombreAutoAnalisis}")
                        .setFontSize(fontSize)
                )
            )
            firmaTable.addCell(
                Cell(2, 2).add(firmaAutorizaImg)
            )

            firmaTable.addCell(
                Cell().add(
                    Paragraph("NOMBRE: ${datosFirma.nombreMuestreador}")
                        .setFontSize(fontSize)
                )
            )
            firmaTable.addCell(
                Cell(2, 2).add(firmaMuestreadorImg)
            )

            document.add(firmaTable)

            document.close()

            Toast.makeText(
                context,
                "PDF generado: ${file.name}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Log.e("PDF_ERROR", e.toString())
            Toast.makeText(context, "Error PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun enqueueSendEmailTaskPlan(
        context: Context,
        emailAddress: String,
        filePath: String,
        folio: String,
        folioExtra: Boolean
    ) {

        Log.d("SendEmailTask", "📧 Enviando correo con PDF: $filePath")

        val mensaje: String = if (folioExtra) {
            """
        <p>- Servicios que generan valor -</p>

        <p>
            <img src="https://grupolesaa.com.mx/img/logorectangulartrans.png" 
                 alt="Logo Lesaa" width="300"/>
        </p>

        <p><strong>¡Tenemos información para ti!</strong></p>

        <p>Buen día,</p>

        <p>
            Esperando que se encuentre bien el día de hoy, le notificamos que ha recibido
            reporte de servicio correspondiente a la colecta de muestras del día
            <strong>${LocalDate.now()}</strong> con No. de folio
            <strong>${folio}E</strong>, el cual se encuentra en proceso y garantizamos su
            terminación en tiempo y forma.
        </p>

        <p>Sin más por el momento, quedamos a sus órdenes.</p>

        <p>¡Que tenga un excelente día!</p>

        <p style="font-size:0.6rem; color:gray">
            Tablet: ${Build.MODEL.uppercase()}
        </p>
        """.trimIndent()
        } else {
            """
        <p>- Servicios que generan valor -</p>

        <a href="https://grupolesaa.com.mx">
            <img src="https://grupolesaa.com.mx/img/logorectangulartrans.png" 
                 alt="Logo Lesaa" width="300"/>
        </a>

        <p><strong>¡Tenemos información para ti!</strong></p>

        <p>Buen día,</p>

        <p>
            Esperando que se encuentre bien el día de hoy, le notificamos que ha recibido
            reporte de servicio correspondiente a la colecta de muestras del día
            <strong>${LocalDate.now()}</strong> con No. de folio
            <strong>$folio</strong>, el cual se encuentra en proceso y garantizamos su
            terminación en tiempo y forma.
        </p>

        <p>Sin más por el momento, quedamos a sus órdenes.</p>

        <p>¡Que tenga un excelente día!</p>

        <p style="font-size:0.6rem; color:gray">
            Tablet: ${Build.MODEL.uppercase()}
        </p>
        """.trimIndent()
        }

        val data = Data.Builder()
            .putString("emailAddress", emailAddress)
            .putString("filePath", filePath)
            .putString("subject", "Reporte de servicio | GRUPO LESAA")
            .putString("messageText", mensaje)
            .putBoolean("isHtml", true)
            .build()

        val sendEmailWorkRequest =
            OneTimeWorkRequestBuilder<SendEmailWorker>()
                .setInputData(data)
                .build()

        WorkManager.getInstance(context).enqueue(sendEmailWorkRequest)
    }



    private fun generarPDFsFirmados(datosFirma: DatosFirmaPlan) {

        foliosSeleccionados.forEach { muestraData ->

            savePdfPlan(
                context = this,
                muestraData = muestraData,
                datosFirma = datosFirma
            )

            enqueueSendEmailTaskPlan(
                context = this,
                emailAddress = datosFirma.correo,
                filePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)}/Muestras-Folio-${muestraData.folio}.pdf",
                folio = muestraData.folio,
                folioExtra = false
            )

        }

        Toast.makeText(
            this,
            "Plan firmado correctamente (${foliosSeleccionados.size} folios)",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }



}
