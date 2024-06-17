package com.example.aplicacionlesaa

import SendEmailWorker
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplicacionlesaa.adapter.muestraAdapterActResumen
import com.example.aplicacionlesaa.databinding.ActivityMain2Binding
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import android.provider.Settings
import com.example.aplicacionlesaa.model.ClientePdm
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.aplicacionlesaa.model.FolioMuestreo
import com.example.aplicacionlesaa.utils.NetworkUtils
import java.time.LocalDate
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import com.example.aplicacionlesaa.model.Muestra_pdm
import com.example.aplicacionlesaa.worker.SendDataWorker


class MainActivity2 : AppCompatActivity() {

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
            try {
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
                    val file = File(pdfPath, "Muestras.pdf")
                    enqueueSendEmailTask(this, "ray.contacto06@gmail.com",
                        "$pdfPath/Muestras.pdf")
                }



                checkStoragePermissionAndSavePdf()
                enqueueSendEmailTask(this,
                    "ray.contacto06@gmail.com",
                    "$pdfPath/Muestras.pdf"
                )
            } catch (e: Exception) {
                Log.i("Error:", e.toString())
            }


            //SendEmailTask("mrlatosta@gmail.com").execute()
        }

        val btnClear = binding.btnClear
        val btnSaveSignature = binding.btnSaveSignature
        val signatureView = binding.signatureView

        btnClear.setOnClickListener {
            signatureView.clear()
        }

        btnSaveSignature.setOnClickListener {
            val signatureBitmap = signatureView.getSignatureBitmap()
            saveBitmap(signatureBitmap)
        }


    }

    private fun sendMuestrasToApi(muestraListaNueva: List<Muestra_pdm>) {
        Log.i("Muestras son:", muestraListaNueva.toString())
        for (muestra in muestraListaNueva) {
            Log.i("Muestras son:", muestra.toString())
            val call = RetrofitClient.instance.createMuestreo(muestra)
            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Muestras enviadas con éxito", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "Error al enviar muestras", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(applicationContext, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })

        }

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


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == storagePermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun savePdf(emailAddress: String) {
        val pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            .toString()
        val file = File(pdfPath, "Muestras.pdf")

        try {
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            for (muestra in muestraMutableList) {
                document.add(Paragraph("Número de Muestra: ${muestra.numeroMuestra}"))
                document.add(Paragraph("Fecha de Muestra: ${muestra.fechaMuestra}"))
                document.add(Paragraph("Hora de Muestra: ${muestra.horaMuestra}"))
                document.add(Paragraph("Registro de Muestra: ${muestra.registroMuestra}"))
                document.add(Paragraph("Nombre de Muestra: ${muestra.nombreMuestra}"))
                document.add(Paragraph("ID de Lab: ${muestra.idLab}"))
                document.add(Paragraph("Cantidad Aproximada: ${muestra.cantidadAprox}"))
                document.add(Paragraph("Temperatura: ${muestra.tempM}"))
                document.add(Paragraph("Lugar de Toma: ${muestra.lugarToma}"))
                document.add(Paragraph("Descripción: ${muestra.descripcionM}"))
                document.add(Paragraph("Estudios Microbiologicos: ${muestra.emicro}"))
                document.add(Paragraph("Estudios Fisicoquimicos: ${muestra.efisico}"))
                document.add(Paragraph("Observaciones: ${muestra.observaciones}"))

                document.add(Paragraph("-------------------------------------------------------------"))
            }

            document.close()
            Toast.makeText(this, "PDF saved at $pdfPath/Muestras.pdf", Toast.LENGTH_LONG).show()

            // Enviar el PDF por correo electrónico
            //SendEmailWorker(emailAddress, file).execute()
            //SendEmailTask("atencionaclienteslab.lesa@gmail.com", file).execute()

        } catch (e: Exception) {
            Log.e("PDF", "Error saving PDF", e)
            Toast.makeText(this, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
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

    private fun sendDataToApi(folioMuestreo: FolioMuestreo) {
        Log.d("sendDataToApi", "Enviando datos: $folioMuestreo")

        val call = RetrofitClient.instance.createFolioMuestreo(folioMuestreo)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("sendDataToApi", "Datos enviados con éxito, respuesta: ${response.body()}")
                    Toast.makeText(
                        this@MainActivity2,
                        "Datos enviados con éxito",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.e("sendDataToApi", "Error al enviar datos, código: ${response.code()}, mensaje: ${response.message()}")
                    Toast.makeText(this@MainActivity2, "Error al enviar datos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("sendDataToApi", "Error de red: ${t.message}", t)
                Toast.makeText(this@MainActivity2, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun enqueueSendEmailTask(context: Context, emailAddress: String, filePath: String) {
        val data = Data.Builder()
            .putString("emailAddress", emailAddress)
            .putString("filePath", filePath)
            .putString("subject","Envio de muestras realizadas")
            .putString("messageText","Le hago entrega de las muestras realizadas correspondiente al dia ${LocalDate.now()} y el folio de muestreo: ${binding.tvFolio.text}. Le agradecemos su preferencia")
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




}
