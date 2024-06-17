package com.example.aplicacionlesaa

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
import com.example.aplicacionlesaa.worker.SendDataWorker


class MainActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityMain2Binding
    private lateinit var muestraMutableList: MutableList<Muestra>
    private lateinit var adapter: muestraAdapterActResumen
    private val storagePermissionRequestCode = 1001
    private var clientePdm: ClientePdm? = null
    private var pdmSeleccionado: String = ""
    val apiService = RetrofitClient.instance


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

        binding.tvCliente.text = clientePdm?.nombre_empresa
        binding.tvPDM.text = pdmSeleccionado


        val folio_cliente = clientePdm?.folio

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

                if (NetworkUtils.isInternetAvailable(this)) {
                    Log.i("Internet", "Si hay internet")
                    sendDataToApi(folioMuestreo)
                } else {
                    Log.i("Internet", "No hay internet")
                    val data = Data.Builder()
                        .putString("folio", folioMuestreo.folio)
                        .putString("fecha", folioMuestreo.fecha)
                        .putString("folio_cliente", folioMuestreo.folio_cliente)
                        .putString("folio_pdm", folioMuestreo.folio_pdm)
                        .build()

                    val workRequest = OneTimeWorkRequestBuilder<SendDataWorker>()
                        .setInputData(data)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()

                    WorkManager.getInstance(this).enqueue(workRequest)
                }


                checkStoragePermissionAndSavePdf()
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
                savePdfAndSendEmail("ray.contacto06@gmail.com")
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
                savePdfAndSendEmail("ray.contacto06@gmail.com")
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
                savePdfAndSendEmail("ray.contacto06@gmail.com")
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
                    savePdfAndSendEmail("ray.contacto06@gmail.com")
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun savePdfAndSendEmail(emailAddress: String) {
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
            SendEmailTask(emailAddress, file).execute()
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



}
