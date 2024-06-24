package com.example.aplicacionlesaa

import RetrofitClient
import SendEmailWorker
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aplicacionlesaa.adapter.muestraAdapterActResumen
import com.example.aplicacionlesaa.api.ApiService
import com.example.aplicacionlesaa.databinding.ActivityMain2Binding
import com.example.aplicacionlesaa.model.ClientePdm
import com.example.aplicacionlesaa.model.DatosFinalesFolioMuestreo
import com.example.aplicacionlesaa.model.FolioMuestreo
import com.example.aplicacionlesaa.model.MuestraData
import com.example.aplicacionlesaa.model.Muestra_pdm
import com.example.aplicacionlesaa.utils.NetworkUtils
import com.example.aplicacionlesaa.worker.SendDataWorker
import com.example.aplicacionlesaa.worker.SendDatosFaltantesWorker
import com.google.gson.Gson
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate


class MainActivity2 : AppCompatActivity(),SignatureDialogFragment.SignatureDialogListener,SignatureDialogFragmentDos.SignatureDialogListener  {

    private lateinit var binding: ActivityMain2Binding
    private lateinit var muestraMutableList: MutableList<Muestra>
    private lateinit var adapter: muestraAdapterActResumen
    private val storagePermissionRequestCode = 1001
    private var clientePdm: ClientePdm? = null
    private var pdmSeleccionado: String = ""
    val apiService = RetrofitClient.instance
    private var folio: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        muestraMutableList = intent.getParcelableArrayListExtra("muestraList") ?: mutableListOf()

        val apiService = RetrofitClient.instance



        initRecyclerView()
        enableEdgeToEdge()
        //setContentView(R.layout.activity_main2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btnInsertSignature = binding.btnInsertSignature
        val btnInsertSignatureDos = binding.btnInsertSignatureDos


        btnInsertSignature.setOnClickListener {
            val signatureDialog = SignatureDialogFragment()
            signatureDialog.setSignatureDialogListener(this)
            signatureDialog.show(supportFragmentManager, "SignatureDialogFragment")
        }

        btnInsertSignatureDos.setOnClickListener {
            val signatureDialogDos = SignatureDialogFragmentDos()
            signatureDialogDos.setSignatureDialogListenerDos(this)
            signatureDialogDos.show(supportFragmentManager, "SignatureDialogFragmentDos")
        }




        clientePdm = intent.getParcelableExtra("clientePdm")
        pdmSeleccionado = intent.getStringExtra("plandemuestreo") ?: "Error"
        folio = intent.getStringExtra("folio")
        binding.tvCliente.text = clientePdm?.nombre_empresa
        binding.tvPDM.text = pdmSeleccionado
        binding.tvFolio.text = folio




        val folio_cliente = clientePdm?.folio
        val muestraListaNueva = convertirAMuestraPdm(muestraMutableList)

        Log.i("Ray", muestraMutableList.toString())
        val btnAceptar = binding.btnAceptar
        btnAceptar.setOnClickListener {

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmación")
            builder.setMessage("¿Estás seguro de que enviar y concluir el folio ${binding.tvFolio.text}?")

            // Configurar el botón "Sí"
            builder.setPositiveButton("Sí") { dialog, which ->

                try {
                    val nombreArchivoPdf = "Muestras-Folio-${binding.tvFolio.text}.pdf"
                    /*try{
                        //saveDataToJson(this, muestraData,"Datos-folio-${binding.tvFolio.text}.json")
                        Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
                        }catch (e: Exception){
                            Log.i("Error guardand0:", e.toString())
                    }*/

                    val folioMuestreo = FolioMuestreo(
                        folio = binding.tvFolio.text.toString(),
                        fecha = LocalDate.now().toString(),
                        folio_cliente = folio_cliente.toString(),
                        folio_pdm = pdmSeleccionado
                    )
                    val pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                        .toString()

                    if (NetworkUtils.isInternetAvailable(this)) {
                        Log.i("Internet", "Si hay internet")

                        Toast.makeText(this, "Si hay internet, enviando muestras", Toast.LENGTH_SHORT).show()
                        sendMuestrasToApi(muestraListaNueva)
                        enqueueSendEmailTask(this,
                            "ray.contacto06@gmail.com",
                            "$pdfPath/$nombreArchivoPdf"
                        )
                        sendDatosFaltantesToApi()
                        /*enqueueSendEmailTask(this, "atencionaclienteslab.lesa@gmail.com",
                            "$pdfPath/$nombreArchivoPdf")*/
                        try{
                            val correo = binding.txtCorreo.text.toString()
                            enqueueSendEmailTask(this, correo,
                                "$pdfPath/$nombreArchivoPdf")
                        }catch (e: Exception){
                            Log.i("Error:", e.toString())
                        }
                    } else {
                        Toast.makeText(this, "No hay internet, los datos se enviarán cuando se establezca una conexión", Toast.LENGTH_SHORT).show()
                        Log.i("Internet", "No hay internet")
                        val tamaño = muestraListaNueva.size

                        // Crear una lista de Data para cada muestra en muestraMutableList
                        val dataList = mutableListOf<Data>()
                        muestraListaNueva.forEachIndexed { index, muestra ->
                            val data = Data.Builder()
                                .putInt("muestra_count",tamaño)
                                .putString("registro_muestra_$index", muestra.registro_muestra)
                                .putString("folio_muestreo_$index", muestra.folio_muestreo)
                                .putString("fecha_muestreo_$index", muestra.fecha_muestreo)
                                .putString("hora_muestreo_$index", muestra.hora_muestreo)
                                .putString("nombre_muestra_$index", muestra.nombre_muestra)
                                .putString("id_lab_$index", muestra.id_lab)
                                .putString("cantidad_aprox_$index", muestra.cantidad_aprox)
                                .putString("temperatura_$index", muestra.temperatura)
                                .putString("lugar_toma_$index", muestra.lugar_toma)
                                .putString("descripcion_toma_$index", muestra.descripcion_toma)
                                .putString("e_micro_$index", muestra.e_micro)
                                .putString("e_fisico_$index", muestra.e_fisico)
                                .putString("observaciones_$index", muestra.observaciones)
                                .putString("folio_pdm_$index", muestra.folio_pdm)
                                .putInt("servicio_id_$index", muestra.servicio_id)
                                .build()

                            dataList.add(data)
                        }

                        // Crear y enviar las tareas programadas para cada muestra en muestraMutableList
                        dataList.forEach { data ->
                            val workRequest = OneTimeWorkRequestBuilder<SendDataWorker>()
                                .setInputData(data)
                                .setConstraints(
                                    Constraints.Builder()
                                        .setRequiredNetworkType(NetworkType.CONNECTED)
                                        .build()
                                )
                                .build()

                            Log.i("Datos", "Entre al worker")
                            WorkManager.getInstance(this).enqueue(workRequest)
                        }


                        // Envío de correo con el archivo PDF
                        val file = File(pdfPath, nombreArchivoPdf)
                        /*enqueueSendEmailTask(this, "atencionaclienteslab.lesa@gmail.com",
                            "$pdfPath/$nombreArchivoPdf")*/
                        enqueueSendEmailTask(this, "ray.contacto06@gmail.com",
                            "$pdfPath/$nombreArchivoPdf")

                        val nombreAutoAnalisis = binding.txtNombreAutoAnalisis.text.toString()
                        val puestoAutoAnalisis = binding.txtPuestoAutoAnalisis.text.toString()
                        val nombreMuestreador = binding.txtNombreMuestreador.text.toString()
                        val puestoMuestreador = binding.txtPuestoMuestreador.text.toString()
                        val folioText = binding.tvFolio.text.toString()

                        // Crear Data object para pasar los datos al Worker
                        val datosFaltantesData = Data.Builder()
                            .putString("nombreAutoAnalisis", nombreAutoAnalisis)
                            .putString("puestoAutoAnalisis", puestoAutoAnalisis)
                            .putString("nombreMuestreador", nombreMuestreador)
                            .putString("puestoMuestreador", puestoMuestreador)
                            .putString("folioText", folioText)
                            .build()

                        println("Datos son: ${nombreAutoAnalisis},${puestoAutoAnalisis},${nombreMuestreador},${puestoMuestreador},${folioText}")

                        val sendDataWorkRequest = OneTimeWorkRequestBuilder<SendDatosFaltantesWorker>()
                            .setInputData(datosFaltantesData)
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build()
                            )
                            .build()

                        // Encolar la solicitud de trabajo
                        WorkManager.getInstance(this).enqueue(sendDataWorkRequest)


                    }



                    checkStoragePermissionAndSavePdf()


                } catch (e: Exception) {
                    Log.i("Error:", e.toString())
                }

            }

            // Configurar el botón "No"
            builder.setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }

            // Mostrar el cuadro de diálogo
            builder.show()




            //SendEmailTask("mrlatosta@gmail.com").execute()
        }

//        val btnClear = binding.btnClear
//        val btnSaveSignature = binding.btnInsertSignature
//        val signatureView = binding.signatureView
//
//        btnClear.setOnClickListener {
//            signatureView.clear()
//        }
//
//        btnSaveSignature.setOnClickListener {
//            val signatureBitmap = signatureView.getSignatureBitmap()
//            saveBitmap(signatureBitmap)
//        }


    }

    /*private fun restarServicio(id: Int, cantidad: Int) {
        val data = ApiService.RestarServicioRequest(cantidad)
        val call = RetrofitClient.instance.restarServicio(id, data)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("restarServicio", "Servicio actualizado correctamente.")
                    Toast.makeText(this@MainActivity2, "Servicio actualizado correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("restarServicio", "Error al actualizar servicio, código: ${response.code()}, mensaje: ${response.message()}")
                    Toast.makeText(this@MainActivity2, "Error al actualizar servicio", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("restarServicio", "Error de red: ${t.message}", t)
                Toast.makeText(this@MainActivity2, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }*/


    private fun sendMuestrasToApi(muestraListaNueva: List<Muestra_pdm>) {
        Log.i("Muestras son:", muestraListaNueva.toString())
        for (muestra in muestraListaNueva) {
            Log.i("Muestras son:", muestra.toString())

            // Enviar la muestra a la API
            val callCreateMuestreo = RetrofitClient.instance.createMuestreo(muestra)
            callCreateMuestreo.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Muestra enviada con éxito", Toast.LENGTH_SHORT).show()

                        // Preparar la solicitud de actualización
                        val restarServicioRequest =
                            ApiService.RestarServicioRequest(cantidad = 1) // O la cantidad que desees restar

                        // Actualizar la cantidad del servicio
                        val callUpdateServicio = RetrofitClient.instance.restarServicio(muestra.servicio_id, restarServicioRequest)
                        callUpdateServicio.enqueue(object : Callback<Void> {
                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                if (response.isSuccessful) {
                                    Toast.makeText(applicationContext, "Cantidad actualizada con éxito", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(applicationContext, "Error al actualizar cantidad", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                Toast.makeText(applicationContext, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Toast.makeText(applicationContext, "Error al enviar muestra", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(applicationContext, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }


    private fun sendDatosFaltantesToApi(){
        val datos = DatosFinalesFolioMuestreo(
            binding.txtNombreAutoAnalisis.text.toString(),
            binding.txtPuestoAutoAnalisis.text.toString(),
            binding.txtNombreMuestreador.text.toString(),
            binding.txtPuestoMuestreador.text.toString()
        )
        val folioText = binding.tvFolio.text.toString()
        val callDatosFaltantes = RetrofitClient.instance.completarFolio(folioText,datos)
        callDatosFaltantes.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "Folio completado Con exito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "Error al completar Folio", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(applicationContext, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun initRecyclerView() {
        //val recyclerView = findViewById<RecyclerView>(R.id.recyclerMuestras)
        //adapter = muestraAdapter(){}
        adapter = muestraAdapterActResumen(
            muestraList = muestraMutableList,
            onClickListener = { muestra -> onItemSelected(muestra) },
            onclickDelete = { position -> onDeletedItem(position) })

        binding.recyclerResumen.layoutManager = LinearLayoutManager(this)
        binding.recyclerResumen.adapter = adapter

    }

    private fun onItemSelected(muestra: Muestra) {
        Toast.makeText(this, muestra.nombreMuestra, Toast.LENGTH_SHORT).show()
        Log.i("Ray", muestra.nombreMuestra)
    }

    private fun onDeletedItem(position: Int) {
        println("Funcion desactivada")
    }

    private fun checkStoragePermissionAndSavePdf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + applicationContext.packageName)
                startActivityForResult(intent, storagePermissionRequestCode)
            } else {
                val muestraData = MuestraData(binding.tvFolio.text.toString(), pdmSeleccionado, muestraMutableList)
                saveDataToJson(this, muestraData,"Datos-folio-${binding.tvFolio.text}.json")
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
                val muestraData = MuestraData(binding.tvFolio.text.toString(), pdmSeleccionado, muestraMutableList)
                saveDataToJson(this, muestraData,"Datos-folio-${binding.tvFolio.text}.json")
                                savePdf("ray.contacto06@gmail.com")
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == storagePermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val muestraData = MuestraData(binding.tvFolio.text.toString(), pdmSeleccionado, muestraMutableList)
                saveDataToJson(this, muestraData,"Datos-folio-${binding.tvFolio.text}.json")
                savePdf("ray.contacto06@gmail.com")

            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == storagePermissionRequestCode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    savePdf("ray.contacto06@gmail.com")
                    val muestraData = MuestraData(binding.tvFolio.text.toString(), pdmSeleccionado, muestraMutableList)
                    saveDataToJson(this, muestraData,"Datos-folio-${binding.tvFolio.text}.json")
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun savePdf(emailAddress: String) {
        val pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val file = File(pdfPath, "Muestras-Folio-${binding.tvFolio.text}.pdf")

        try {
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument, PageSize.A4.rotate())

            document.add(Paragraph("El folio es: ${binding.tvFolio.text}"))
            document.add(Paragraph("-------------------------------------------------------------"))
            document.add(Paragraph(clientePdm?.nombre_empresa))
            document.add(Paragraph(clientePdm?.direccion))
            document.add(Paragraph(clientePdm?.telefono))
            document.add(Paragraph(clientePdm?.correo))
            document.add(Paragraph(clientePdm?.atencion))
            document.add(Paragraph(clientePdm?.puesto))
            document.add(Paragraph(clientePdm?.departamento))
            document.add(Paragraph("La fecha es: ${LocalDate.now()}"))
            document.add(Paragraph("-------------------------------------------------------------"))
            document.add(Paragraph("Muestras Realizadas"))

            // Crear la tabla
            val table = Table(floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f))
            table.setWidth(UnitValue.createPercentValue(100f))

            // Agregar encabezados de celda
            addTableHeader(table)

            // Agregar filas de datos
            for (muestra in muestraMutableList) {
                addTableRow(table, muestra)
            }

            // Configurar el tamaño de fuente para las celdas
            table.setFontSize(8f)

            document.add(table)
            document.close()

            Toast.makeText(this, "PDF saved at $pdfPath/Muestras-Folio-${binding.tvFolio.text}.pdf", Toast.LENGTH_LONG).show()

            // Enviar el PDF por correo electrónico
            // SendEmailWorker(emailAddress, file).execute()
            // SendEmailTask(emailAddress, file).execute()

        } catch (e: Exception) {
            Toast.makeText(this, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }

    }

    private fun addTableHeader(table: Table) {
        val headers = arrayOf(
            "Número de Muestra", "Fecha de Muestra",
            "Registro de Muestra", "Nombre de Muestra", "ID de Lab",
            "Cantidad Aproximada", "Temperatura", "Lugar de Toma",
            "Descripción", "Estudios Microbiológicos", "Estudios Fisicoquímicos",
            "Observaciones"
        )

        headers.forEach {
            val headerCell = Cell().add(Paragraph(it))
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(DeviceRgb(30,19,51)).setFontColor(DeviceRgb(255,255,255))
            table.addHeaderCell(headerCell)
        }
    }

    private fun addTableRow(table: Table, muestra: Muestra) {
        table.addCell(muestra.numeroMuestra)
        table.addCell(muestra.fechaMuestra)
        table.addCell(muestra.registroMuestra)
        table.addCell(muestra.nombreMuestra)
        table.addCell(muestra.idLab)
        table.addCell(muestra.cantidadAprox)
        table.addCell(muestra.tempM)
        table.addCell(muestra.lugarToma)
        table.addCell(muestra.descripcionM)
        table.addCell(muestra.emicro)
        table.addCell(muestra.efisico)
        table.addCell(muestra.observaciones)
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .toString() + "/signature.png"
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = FileOutputStream(filePath)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            Toast.makeText(this, "Signature saved at $filePath", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving signature: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun enqueueSendEmailTask(context: Context, emailAddress: String, filePath: String) {
        val data = Data.Builder()
            .putString("emailAddress", emailAddress)
            .putString("filePath", filePath)
            .putString("subject","Solicitud de servicio GRUPO LESAA")
            .putString("messageText","Hola ${clientePdm?.atencion} \n\n" +
                    "Reciba un cordial saludo, por este medio le notificamos que ha recibido la solicitud de servicio correspondiente al muestreo del día ${LocalDate.now()} con No. de folio ${binding.tvFolio.text} el cual está en proceso y garantizamos la terminación de este en tiempo y forma. \n"+

        "Sin más por el momento, quedamos a sus órdenes.\n\n"+
        "! Tenga un excelente día ¡"
        )
            .build()

        val sendEmailWorkRequest = OneTimeWorkRequest.Builder(SendEmailWorker::class.java)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(sendEmailWorkRequest)
    }

    fun convertirAMuestraPdm(muestras: List<Muestra>): List<Muestra_pdm> {
        val listaMuestrasPdm = mutableListOf<Muestra_pdm>()

        for (muestra in muestras) {
            val muestraPdm = Muestra_pdm(
                registro_muestra = muestra.registroMuestra,
                folio_muestreo = binding.tvFolio.text.toString(),
                fecha_muestreo = muestra.fechaMuestra,
                hora_muestreo = muestra.horaMuestra,
                nombre_muestra = muestra.nombreMuestra,
                id_lab = muestra.idLab,
                cantidad_aprox = muestra.cantidadAprox,
                temperatura = muestra.tempM,
                lugar_toma = muestra.lugarToma,
                descripcion_toma = muestra.descripcionM,
                e_micro = muestra.emicro,
                e_fisico = muestra.efisico,
                observaciones = muestra.observaciones,
                folio_pdm = binding.tvPDM.text.toString(),
                servicio_id = muestra.servicioId
            )
            listaMuestrasPdm.add(muestraPdm)
        }

        return listaMuestrasPdm
    }

    override fun onSignatureSaved(bitmap: Bitmap) {
        var signatureView = binding.signatureViewUno
        signatureView.setSignatureBitmap(bitmap)
    }

    override fun onSignatureSavedDos(bitmap: Bitmap) {
        var signatureViewDos = binding.signatureViewDos
        signatureViewDos.setSignatureBitmap(bitmap)
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





}
