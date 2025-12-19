package com.example.aplicacionlesaa

import RetrofitClient
import SendEmailWorker
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
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
import com.example.aplicacionlesaa.model.Lugar
import com.example.aplicacionlesaa.model.MuestraData
import com.example.aplicacionlesaa.model.Muestra_pdm
import com.example.aplicacionlesaa.model.Muestra_pdmExtra
import com.example.aplicacionlesaa.model.Pdm
import com.example.aplicacionlesaa.model.Servicio
import com.example.aplicacionlesaa.model.analisisFisico
import com.example.aplicacionlesaa.utils.NetworkUtils
import com.example.aplicacionlesaa.worker.SendDataWorker
import com.example.aplicacionlesaa.worker.SendDataWorkerMuestrasExtra
import com.example.aplicacionlesaa.worker.SendDatosFaltantesWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.EnumMap
import com.google.zxing.qrcode.QRCodeWriter
import com.itextpdf.barcodes.BarcodeQRCode
import java.net.URLEncoder


class MainActivity2 : AppCompatActivity(),SignatureDialogFragment.SignatureDialogListener,SignatureDialogFragmentDos.SignatureDialogListener  {

    private lateinit var binding: ActivityMain2Binding
    private lateinit var muestraMutableList: MutableList<Muestra>
    private lateinit var adapter: muestraAdapterActResumen
    private val storagePermissionRequestCode = 1001
    private var clientePdm: ClientePdm? = null
    private var pdmSeleccionado: String = ""
    val apiService = RetrofitClient.instance
    private val serviciosList: MutableList<Servicio> = mutableListOf()
    private var folio: String? = null
    private lateinit var pdmDetallado: Pdm
    private var lugares: ArrayList<String> = ArrayList()
    private var muestrasExtra: MutableList<Muestra> = mutableListOf()
    private var existenMuestrasExtra: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        muestraMutableList = intent.getParcelableArrayListExtra("muestraList") ?: mutableListOf()
        try{
            muestrasExtra = intent.getParcelableArrayListExtra("muestraExtraList") ?: mutableListOf()
            existenMuestrasExtra = true
            Log.i("Muestras extra son:", muestrasExtra.toString())

        }catch (e: Exception){
                Log.i("Error:", e.toString())
        }

        if (muestrasExtra.isNotEmpty()) {
            binding.tvMuestrasExtra.text = "Muestras extra: ${muestrasExtra.size}"
            Log.e("Muestras extra son:", muestrasExtra.toString())
        }else{
            binding.tvMuestrasExtra.text = "Muestras extra: 0"
        }

        val fisicoquimicos = getSavedAnalisisFisicoList()

        Log.e("Los fisicoquimicos", "Los fisimicos son $fisicoquimicos")

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

        btnInsertSignatureDos.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Información")
            builder.setMessage("Recordatorio: Verifica que los lugares de toma, las nombres y las descripciones de las muestras sean correctos. ")
            builder.setPositiveButton("Aceptar") { dialog, which ->
                val signatureDialogDos = SignatureDialogFragmentDos()
                signatureDialogDos.setSignatureDialogListenerDos(this)
                signatureDialogDos.show(supportFragmentManager, "SignatureDialogFragmentDos")
            }

            val dialog = builder.create()
            dialog.show()

        }




        clientePdm = intent.getParcelableExtra("clientePdm")
        pdmSeleccionado = intent.getStringExtra("plandemuestreo") ?: "Error"
        lugares = intent.getStringArrayListExtra("lugares") ?: arrayListOf()
        pdmDetallado = intent.getParcelableExtra("pdmDetallado")!!

        folio = intent.getStringExtra("folio")
        binding.tvCliente.text = clientePdm?.nombre_empresa
        binding.tvPDM.text = pdmSeleccionado
        binding.tvFolio.text = folio


        val serviciosRecibidos = intent.getParcelableArrayListExtra<Servicio>("listaServicios")
        if (serviciosRecibidos != null) {
            serviciosList.addAll(serviciosRecibidos)
        }

        val folio_cliente = clientePdm?.folio
        val muestraListaNueva = convertirAMuestraPdm(muestraMutableList)


        binding.txtCorreo.setText(clientePdm?.correo.toString())
        Log.i("Correo", clientePdm?.correo.toString())

        Log.i("Ray", muestraMutableList.toString())
        val btnAceptar = binding.btnAceptar
        btnAceptar.setOnClickListener {

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmación")
            if (muestrasExtra.isNotEmpty()) {
                builder.setMessage("¿Estás seguro de  enviar y concluir el folio ${binding.tvFolio.text} con muestras extra con folio ${binding.tvFolio.text}E?")
            }else{
                builder.setMessage("¿Estás seguro de  enviar y concluir el folio ${binding.tvFolio.text}?")
            }

            // Configurar el botón "Sí"
            builder.setPositiveButton("Sí") { dialog, which ->

                try {

                    val nombreArchivoPdf = "Muestras-Folio-${binding.tvFolio.text}.pdf"

                    val pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                        .toString()

                    if (NetworkUtils.isInternetAvailable(this)) {
                        Log.i("Internet", "Si hay internet")

                        var lugarMutableList = mutableListOf<Lugar>()
                        for (muestra in muestraMutableList) {
                            if (muestra.lugarToma !in lugares ){
                                val lugar = Lugar(
                                    cliente_folio = clientePdm?.folio.toString(),
                                    nombre_lugar = muestra.lugarToma,
                                    folio_pdm = binding.tvPDM.text.toString())
                                lugarMutableList.add(lugar)
                            }

                        }



                        for (lugar in lugarMutableList) {
                            Log.e("LugarAkirau:", lugar.nombre_lugar + lugar.cliente_folio+ lugar.folio_pdm)
                            val callCreateLugar = RetrofitClient.instance.createLugarCliente(lugar)
                            callCreateLugar.enqueue(object : Callback<Void> {
                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(applicationContext, "Lugar enviado con exito", Toast.LENGTH_SHORT).show()

                                    } else {
                                        Log.e("Error:", response.code().toString())
                                        Log.e("Error:", response.message())
                                        Toast.makeText(applicationContext, "Error al enviar Lugar", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onFailure(call: Call<Void>, t: Throwable) {
                                    Toast.makeText(applicationContext, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }

                        Toast.makeText(this, "Si hay internet, enviando muestras", Toast.LENGTH_SHORT).show()
                        val tamaño = muestraListaNueva.size

                        // Crear una lista de Data para cada muestra en muestraMutableList
                        // 🔹 NUEVO: solo un envío bulk
//                        val gson = Gson()
                        val fechaHoy = LocalDate.now().toString() // Formato YYYY-MM-DD
                        val filename = "Datos-folio-${binding.tvFolio.text}-${fechaHoy}.json"
                        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString()
                        val filePath = "$documentsDir/$filename"

//                        val muestrasJson = gson.toJson(muestraListaNueva)

                        val data = Data.Builder()
                            .putString("filePath", filePath)
                            .putString("folioText", binding.tvFolio.text.toString())
                            .putString("nombreAutoAnalisis", binding.txtNombreAutoAnalisis.text.toString())
                            .putString("puestoAutoAnalisis", binding.txtPuestoAutoAnalisis.text.toString())
                            .putString("nombreMuestreador", binding.txtNombreMuestreador.text.toString())
                            .putString("puestoMuestreador", binding.txtPuestoMuestreador.text.toString())
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
                        Log.i("MainActivity2", "📦 Enviando ${muestraListaNueva.size} muestras en una sola solicitud bulk")
                        enqueueSendEmailTask(this,
                            "operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com",
                            "$pdfPath/$nombreArchivoPdf",
                            false
                        )
                        sendDatosFaltantesToApi()
                        binding.textView6.text = "Resumen de muestras - Folio concluido"
                        /*enqueueSendEmailTask(this, "atencionaclienteslab.lesa@gmail.com",
                            "$pdfPath/$nombreArchivoPdf")*/
                        try{
                            val correo = binding.txtCorreo.text.toString()
                            enqueueSendEmailTask(this, correo,
                                "$pdfPath/$nombreArchivoPdf",
                                false)

                            if (muestrasExtra.isNotEmpty()) {
                                val nombreArchivoPdfExt = "Muestras-Folio-${binding.tvFolio.text}E.pdf"

                                enqueueSendEmailTask(this, correo,
                                    "$pdfPath/$nombreArchivoPdfExt",
                                    true)
                            }


                        }catch (e: Exception){
                            Log.i("Error:", e.toString())
                        }

                        if (muestrasExtra.isNotEmpty()) {

                            val muestraListaNuevaExtra = convertirAMuestraPdmExtra(muestrasExtra)


                            val tamanoExtra = muestraListaNuevaExtra.size


                            // Crear una lista de Data para cada muestra en muestraMutableList
                            val dataListExtra = mutableListOf<Data>()
                            //Envio de lista de muestra
                            muestraListaNuevaExtra.forEachIndexed { index, muestra ->
                                val data = Data.Builder()
                                    .putInt("muestra_count",tamanoExtra)
                                    .putString("registro_muestra_$index", muestra.registro_muestra)
                                    .putString("folio_muestreo_$index", muestra.folio_muestreo + "E")
                                    .putString("fecha_muestreo_$index", muestra.fecha_muestreo)
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
                                    .putInt("estudio_id_$index", muestra.estudio_id)
                                    .putString("cliente",clientePdm?.folio)
                                    .putString("folio",binding.tvFolio.text.toString() + "E")
                                    .putString("folioPDM",pdmSeleccionado)
                                    .build()

                                dataListExtra.add(data)
                                Log.e("Muestras extra son:", muestraListaNuevaExtra.toString())
                            }

                            // Crear y enviar las tareas programadas para cada muestra en muestraMutableList
                            dataListExtra.forEach { data ->
                                val workRequest = OneTimeWorkRequestBuilder<SendDataWorkerMuestrasExtra>()
                                    .setInputData(data)
                                    .setConstraints(
                                        Constraints.Builder()
                                            .setRequiredNetworkType(NetworkType.CONNECTED)
                                            .build()
                                    )
                                    .build()

                                Log.i("Si hay internet", "Entre al worker")
                                WorkManager.getInstance(this).enqueue(workRequest)
                            }

                            val nombreArchivoPdfExtra = "Muestras-Folio-${binding.tvFolio.text}E.pdf"

                            enqueueSendEmailTask(this, "operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com",
                                "$pdfPath/$nombreArchivoPdfExtra",
                                true)

                        }

                        val sharedPreferences = getSharedPreferences("FisicoquimicosPrefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()

            // Borrar todas las preferencias
                        editor.clear()
                        editor.apply()

                        Toast.makeText(this, "Preferencias eliminadas", Toast.LENGTH_SHORT).show()



                    } else {
                        Toast.makeText(this, "No hay internet, los datos se enviarán cuando se establezca una conexión", Toast.LENGTH_SHORT).show()
                        Log.i("Internet", "No hay internet")
                        val tamaño = muestraListaNueva.size

                        // 🔹 NUEVO: solo un envío bulk
//                        val gson = Gson()
//                        val muestrasJson = gson.toJson(muestraListaNueva)
                        val fechaHoy = LocalDate.now().toString() // Formato YYYY-MM-DD
                        val filename = "Datos-folio-${binding.tvFolio.text}-${fechaHoy}.json"
                        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString()
                        val filePath = "$documentsDir/$filename"

//                        val muestrasJson = gson.toJson(muestraListaNueva)

                        val data = Data.Builder()
                            .putString("filePath", filePath)
                            .putString("folioText", binding.tvFolio.text.toString())
                            .putString("nombreAutoAnalisis", binding.txtNombreAutoAnalisis.text.toString())
                            .putString("puestoAutoAnalisis", binding.txtPuestoAutoAnalisis.text.toString())
                            .putString("nombreMuestreador", binding.txtNombreMuestreador.text.toString())
                            .putString("puestoMuestreador", binding.txtPuestoMuestreador.text.toString())
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
                        Log.i("MainActivity2", "📦 Enviando ${muestraListaNueva.size} muestras en una sola solicitud bulk")




                        // Envío de correo con el archivo PDF
                        val file = File(pdfPath, nombreArchivoPdf)
                        /*enqueueSendEmailTask(this, "atencionaclienteslab.lesa@gmail.com",
                            "$pdfPath/$nombreArchivoPdf")*/
                        enqueueSendEmailTask(this, "operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com",
                            "$pdfPath/$nombreArchivoPdf",false)

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

                        if (muestrasExtra.isNotEmpty()) {

                            val muestraListaNuevaExtra = convertirAMuestraPdmExtra(muestrasExtra)


                            val tamanoExtra = muestraListaNuevaExtra.size
                            Log.e("El tamanoextra es:", tamanoExtra.toString())

                            // Crear una lista de Data para cada muestra en muestraMutableList
                            val dataListExtra = mutableListOf<Data>()
                            //Envio de lista de muestra
                            muestraListaNuevaExtra.forEachIndexed { index, muestra ->
                                val data = Data.Builder()
                                    .putInt("muestra_count",tamanoExtra)
                                    .putString("registro_muestra_$index", muestra.registro_muestra)
                                    .putString("folio_muestreo_$index", muestra.folio_muestreo + "E")
                                    .putString("fecha_muestreo_$index", muestra.fecha_muestreo)
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
                                    .putInt("estudio_id_$index", muestra.estudio_id)
                                    .putString("cliente",clientePdm?.folio)
                                    .putString("folio",binding.tvFolio.text.toString() + "E")
                                    .putString("folioPDM",pdmSeleccionado)
                                    .build()

                                dataListExtra.add(data)
                            }

                            // Crear y enviar las tareas programadas para cada muestra en muestraMutableList
                            dataListExtra.forEach { data ->
                                val workRequest = OneTimeWorkRequestBuilder<SendDataWorkerMuestrasExtra>()
                                    .setInputData(data)
                                    .setConstraints(
                                        Constraints.Builder()
                                            .setRequiredNetworkType(NetworkType.CONNECTED)
                                            .build()
                                    )
                                    .build()

                                Log.i("Si hay internet", "Entre al worker")
                                WorkManager.getInstance(this).enqueue(workRequest)
                            }

                            val nombreArchivoPdfExtra = "Muestras-Folio-${binding.tvFolio.text}E.pdf"

                            enqueueSendEmailTask(this, "operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com",
                                "$pdfPath/$nombreArchivoPdfExtra",true)


                            val sharedPreferences = getSharedPreferences("FisicoquimicosPrefs", Context.MODE_PRIVATE)
                            val editor = sharedPreferences.edit()

                            // Borrar todas las preferencias
                            editor.clear()
                            editor.apply()

                            Toast.makeText(this, "Preferencias eliminadas", Toast.LENGTH_SHORT).show()


                        }



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

        binding.btnVme.setOnClickListener {

            if (muestrasExtra.isNotEmpty()) {
                val intent = Intent(this, VerMuestrasExtraActivity::class.java)
                intent.putParcelableArrayListExtra("muestraExtraList", ArrayList(muestrasExtra))
                intent.putExtra("clientePdm", clientePdm)
                intent.putExtra("plandemuestreo", pdmSeleccionado)
                intent.putExtra("pdmDetallado", pdmDetallado)
                intent.putExtra("folio",folio)
                startActivity(intent)
            }else{
                Toast.makeText(this, "No hay muestras extra", Toast.LENGTH_SHORT).show()
            }
        }


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
                        val callUpdateServicio = RetrofitClient.instance.restarServicio(muestra.servicio_id!!, restarServicioRequest)
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
                val muestraData = MuestraData(
                    binding.tvFolio.text.toString(),
                    pdmSeleccionado,
                    clientePdm,
                    serviciosList,
                    muestraMutableList,
                    pdmDetallado,
                    ArrayList(muestrasExtra)
                )
                val fechaHoy = LocalDate.now().toString() // Formato YYYY-MM-DD
                saveDataToJson(this, muestraData, "Datos-folio-${binding.tvFolio.text}-${fechaHoy}.json")
                savePdf("operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com")
                if (muestrasExtra.isNotEmpty()) {
                    savePdfExtra("operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com")
                }
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
                val muestraData = MuestraData(binding.tvFolio.text.toString(),
                    pdmSeleccionado,
                    clientePdm,
                    serviciosList,
                    muestraMutableList,
                    pdmDetallado,
                    ArrayList(muestrasExtra))
                val fechaHoy = LocalDate.now().toString() // Formato YYYY-MM-DD
                saveDataToJson(this, muestraData, "Datos-folio-${binding.tvFolio.text}-${fechaHoy}.json")
                                savePdf("operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com")
                if (muestrasExtra.isNotEmpty()) {
                    savePdfExtra("operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com")
                }
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
                val muestraData = MuestraData(binding.tvFolio.text.toString(),
                    pdmSeleccionado,
                    clientePdm,
                    serviciosList,
                    muestraMutableList,
                    pdmDetallado,
                    ArrayList(muestrasExtra)

                )
                val fechaHoy = LocalDate.now().toString() // Formato YYYY-MM-DD
                saveDataToJson(this, muestraData, "Datos-folio-${binding.tvFolio.text}-${fechaHoy}.json")
                savePdf("operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com")
                if (muestrasExtra.isNotEmpty()) {
                    savePdfExtra("operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com")
                }

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
                    savePdf("operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com")
                    if (muestrasExtra.isNotEmpty()) {
                        savePdfExtra("operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com")
                    }
                    val muestraData = MuestraData(binding.tvFolio.text.toString(),
                        pdmSeleccionado,
                        clientePdm,
                        serviciosList,
                        muestraMutableList,
                        pdmDetallado,
                        ArrayList(muestrasExtra))
                    val fechaHoy = LocalDate.now().toString() // Formato YYYY-MM-DD
                    saveDataToJson(this, muestraData, "Datos-folio-${binding.tvFolio.text}-${fechaHoy}.json")
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
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





    private fun savePdf(emailAddress: String) {

        val pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val file = File(pdfPath, "Muestras-Folio-${binding.tvFolio.text}.pdf")

        try {
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)

            val muestraData = MuestraData(binding.tvFolio.text.toString(),
                pdmSeleccionado,
                clientePdm,
                serviciosList,
                muestraMutableList,
                pdmDetallado,
                ArrayList(muestrasExtra))

            val qrImage = crearQrImage(muestraData, pdfDocument)


            val document = Document(pdfDocument, PageSize.A4.rotate())

            val footerHandler = FooterEventHandler(document)
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler)



//            val qrCode = crearPdfConQrItext(muestraData)


            val inputStream = applicationContext.resources.openRawResource(R.raw.logorectangulartrans)
            val byteArrayOutputStream = ByteArrayOutputStream()
            var nextByte = inputStream.read()
            while (nextByte != -1) {
                byteArrayOutputStream.write(nextByte)
                nextByte = inputStream.read()
            }

            val imageData = byteArrayOutputStream.toByteArray()
            //--handler


            val image = Image(ImageDataFactory.create(imageData))
            image.scaleToFit(125f, 90f)
            //document.add(image)

            // Colores
            // Crear colores para la tabla
            // Crear colores para la tabla
            // Crear colores para la tabla
            val headerColor = DeviceCmyk(99, 38, 0, 67	)
            val subHeaderColor = DeviceRgb(153, 204, 255)
            val whiteColor = DeviceRgb(255, 255, 255)
            val fontSize = 8f // Tamaño de fuente más pequeño

            // Crear tabla principal (3 columnas)
            val mainTable = Table(UnitValue.createPercentArray(floatArrayOf(4f,1f,1f))).useAllAvailableWidth().setBorder(Border.NO_BORDER)

            // Tabla de encabezado (3 columnas)
            val tableEncabezado = Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f))).useAllAvailableWidth().setBorder(Border.NO_BORDER)


            // Encabezado principal
            val mainHeaderCell = Cell(1, 2)
                .add(Paragraph("F-ING-LAB-02. HOJA DE REGISTRO DE CAMPO").setFontColor(whiteColor).setFontSize(fontSize).setBold())
                .setBackgroundColor(headerColor).setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            tableEncabezado.addCell(mainHeaderCell)


            // Sub-encabezado
            val subHeaderCell = Cell(1, 2)
                .add(Paragraph("Servicios que generan valor").setFontColor(whiteColor).setFontSize(fontSize).setBold())
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            tableEncabezado.addCell(subHeaderCell)

            // Crear subtabla para "DATOS DE SOLICITUD" (2 columnas)
            val datosSolicitudTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
            datosSolicitudTable.setWidth(UnitValue.createPercentValue(100f))
            datosSolicitudTable.setHeight(UnitValue.createPercentValue(100f))

            datosSolicitudTable.addCell(Cell().add(Paragraph("AÑO:").setFontSize(fontSize)).setFontColor(whiteColor).setBackgroundColor(headerColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(LocalDate.now().year.toString()).setFontSize(fontSize).setBold())).setBackgroundColor(whiteColor)

            datosSolicitudTable.addCell(Cell().add(Paragraph("MES:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(String.format("%02d", LocalDate.now().monthValue)).setFontSize(fontSize).setBold())).setBackgroundColor(whiteColor)

            datosSolicitudTable.addCell(Cell().add(Paragraph("DÍA:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(String.format("%02d", LocalDate.now().dayOfMonth)).setFontSize(fontSize).setBold())).setBackgroundColor(whiteColor)

            datosSolicitudTable.addCell(Cell().add(Paragraph("FOLIO:").setFontSize(fontSize).setFontColor(whiteColor)).setBackgroundColor(headerColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(binding.tvFolio.text.toString()).setFontSize(fontSize).setBold())).setBackgroundColor(whiteColor)

            // Crear subtabla para "DATOS DE QUIEN SOLICITA LOS ANÁLISIS" (4 columnas)
            val datosSolicitanteTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f))).useAllAvailableWidth()

            datosSolicitanteTable.addCell(Cell().add(Paragraph("NOMBRE:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell(1, 3).add(Paragraph(clientePdm?.nombre_empresa).setFontSize(fontSize).setBold()))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("DIRECCIÓN:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell(1, 3).add(Paragraph(clientePdm?.direccion).setFontSize(fontSize).setBold()))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("ATENCIÓN A:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell().add(Paragraph(clientePdm?.atencion).setFontSize(fontSize).setBold()))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("TELÉFONO:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell().add(Paragraph(clientePdm?.telefono).setFontSize(fontSize).setBold()))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("PUESTO:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell().add(Paragraph(clientePdm?.puesto).setFontSize(fontSize).setBold()))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("CORREO:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell(1, 3).add(Paragraph(clientePdm?.correo).setFontSize(fontSize).setBold()))

            // Agregar sub-tablas a la tabla de encabezado en la misma fila
            tableEncabezado.addCell(Cell(1, 1).add(Paragraph("DATOS DE SOLICITUD").setFontColor(whiteColor)).setBackgroundColor(headerColor).setFontSize(fontSize))
            tableEncabezado.addCell(Cell(1, 2).add(Paragraph("DATOS DE QUIEN SOLICITA LOS ANÁLISIS").setFontColor(whiteColor)).setBackgroundColor(headerColor).setFontSize(fontSize))

            tableEncabezado.addCell(Cell().add(datosSolicitudTable)).setBorder(Border.NO_BORDER)
            tableEncabezado.addCell(Cell(1, 2).add(datosSolicitanteTable)).setBorder(Border.NO_BORDER)

            // Agregar la tabla de encabezado y el logo a la tabla principal
            mainTable.addCell(Cell().add(tableEncabezado).setBorder(Border.NO_BORDER)).setBorder(Border.NO_BORDER)
            mainTable.addCell(Cell().add(qrImage).setVerticalAlignment(VerticalAlignment.MIDDLE).setHorizontalAlignment(HorizontalAlignment.RIGHT).setBorder(Border.NO_BORDER)).setBorder(Border.NO_BORDER)
            mainTable.addCell(Cell().add(image).setVerticalAlignment(VerticalAlignment.MIDDLE).setHorizontalAlignment(HorizontalAlignment.RIGHT).setBorder(Border.NO_BORDER)).setBorder(Border.NO_BORDER)
            // Agregar la tabla principal al documento


            document.add(mainTable)

//            val logoPath = "res/raw/logorectangulartrans.png" // Ruta a tu imagen
//            val img = Image(ImageDataFactory.create(logoPath))
//            document.add(img)
//            document.add(Paragraph("Muestras Realizadas"))

            // Crear la tabla
            val table = Table(floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)).setMarginTop(10f)
            table.setWidth(UnitValue.createPercentValue(100f))

            // Agregar encabezados de celda
            val tabeadercell = Cell(1, 7)
                .add(Paragraph("Datos de las muestras colectadas").setFontColor(whiteColor).setFontSize(10f))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            table.addHeaderCell(tabeadercell)

            val estutablcell = Cell(1, 2)
                .add(Paragraph("Estudios a realizar").setFontColor(whiteColor).setFontSize(10f))
                .setBackgroundColor(DeviceRgb(22, 127, 165))
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

            addTableHeader(table)

            // Agregar filas de datos
            for (muestra in muestraMutableList) {
                addTableRow(table, muestra)
            }

            // Configurar el tamaño de fuente para las celdas
            table.setFontSize(7f)
            document.add(table)
            val paragrafoNomenclaturas = Paragraph("MA=Mesofilicos Aerobios - CT=Coliformes Totales - MH=Mohos - LV=Levaduras - CF=Coliformes Fecales - EC=Escherichia Coli - SA=Staphylococcus Aureus - SS=Salmonella spp - LM=Listeria Monocytogenes Vc=Vibrio Cholerae spp. - VP=Vibrio Parahemolitico spp. - PS=Pseudomona - CP = Clostridium Perfringens = BC=Bacillus Cereus - LP=Legionella spp. -ACA= Acanthamoeba spp. - NAE=Naegleria spp. - EFC=Enterococcus Fecales. - GL=Giardia Lamblia. - CLL=Cloro libre. - CCT= Cloro total.-PH=Potencial de Hidrógeno.-Crnas=Cloraminas,DUR=Dureza.- Alk=Alcalinidad. - SDT=Sólidos Disueltos Totales. - CE-Conductividad Electrica. - TUR=Turbidez CyA").setFontColor(DeviceRgb(1,1,1)).setFontSize(5f)

            document.add(paragrafoNomenclaturas)

            var signatureViewUno = binding.signatureViewUno
            val signatureBitmap = signatureViewUno.getSignatureBitmap()
            val stream = ByteArrayOutputStream()
            signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            // Crear la imagen de iTextPDF a partir del array de bytes
            val imagedata = ImageDataFactory.create(byteArray)
            val signatureImageUno = Image(imagedata).scaleToFit(100f, 100f)

            var signatureViewDos = binding.signatureViewDos
            val signatureBitmapDos = signatureViewDos.getSignatureBitmap()
            val streamDos = ByteArrayOutputStream()
            signatureBitmapDos.compress(Bitmap.CompressFormat.PNG, 100, streamDos)
            val byteArrayDos = streamDos.toByteArray()

            // Crear la imagen de iTextPDF a partir del array de bytes
            val imagedataDos = ImageDataFactory.create(byteArrayDos)
            val signatureImageDos = Image(imagedataDos).scaleToFit(100f, 100f)



            // Crear tabla para la sección de firmas
            var firmaTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f)))
            firmaTable.setWidth(UnitValue.createPercentValue(100f)).setHorizontalAlignment(HorizontalAlignment.CENTER)

            firmaTable.setMarginRight(10f)
// Encabezado
            val headerAutorizaCell = Cell(1, 3)
                .add(Paragraph("QUIÉN AUTORIZA ANÁLISIS DE LAS MUESTRAS").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            val headerFirmaCell1 = Cell()
                .add(Paragraph("FIRMA").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
//                .setBorder(Border.NO_BORDER)

            val headerTomaCell = Cell(1, 3)
                .add(Paragraph("QUIÉN TOMA LAS MUESTRAS").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            val headerFirmaCell2 = Cell()
                .add(Paragraph("FIRMA").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)

            firmaTable.addCell(headerAutorizaCell)
            firmaTable.addCell(headerFirmaCell1)
//

// Datos de quien autoriza
            val autorizaNombreCell = Cell()
                .add(Paragraph("NOMBRE:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)
            val autorizaNombreValueCell = Cell(1,2)
                .add(Paragraph(binding.txtNombreAutoAnalisis.text.toString())).setFontSize(fontSize).setBold()
            val autorizaPuestoCell = Cell()
                .add(Paragraph("PUESTO:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)
            val autorizaPuestoValueCell = Cell(1,2)
                .add(Paragraph(binding.txtPuestoAutoAnalisis.text.toString())).setFontSize(fontSize).setBold()

            firmaTable.addCell(autorizaNombreCell)
            firmaTable.addCell(autorizaNombreValueCell)
            firmaTable.addCell(Cell(2, 2).add(signatureImageUno))
            firmaTable.addCell(autorizaPuestoCell)
            firmaTable.addCell(autorizaPuestoValueCell)

// Datos de quien toma las muestras
            val tomaNombreCell = Cell()
                .add(Paragraph("NOMBRE:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)
            val tomaNombreValueCell = Cell(1, 2)
                .add(Paragraph(binding.txtNombreMuestreador.text.toString()).setFontSize(fontSize).setBold())

            val tomaPuestoCell = Cell()
                .add(Paragraph("PUESTO:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)
            val tomaPuestoValueCell = Cell(1, 2)
                .add(Paragraph("Ing. de campo").setFontSize(fontSize).setBold())

            firmaTable.addCell(headerTomaCell)
            firmaTable.addCell(headerFirmaCell2)
            firmaTable.addCell(tomaNombreCell)
            firmaTable.addCell(tomaNombreValueCell)
            firmaTable.addCell(Cell(2, 2).add(signatureImageDos))
            firmaTable.addCell(tomaPuestoCell)
            firmaTable.addCell(tomaPuestoValueCell)


// Agregar tabla de firmas al documento

            var puntoCriticoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f))).useAllAvailableWidth()
//            puntoCriticoTable.setWidth(UnitValue.createPercentValue(80f)).setHorizontalAlignment(HorizontalAlignment.CENTER)

            val puntoCriticoHeader = Cell(1, 4)
                .add(Paragraph("PUNTO CRITICO PRE - ANALISIS").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)

            puntoCriticoTable.addCell(puntoCriticoHeader)

            val subtablaSalida = Table(UnitValue.createPercentArray(floatArrayOf(1f,1f, 1f, 1f, 1f,1f)))
            subtablaSalida.setWidth(UnitValue.createPercentValue(100f))
            val subtablaSalidaFecha = Cell()
                .add(Paragraph("Fecha:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)
            val subtablaSalidaTemp = Cell()
                .add(Paragraph("Temp:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)

            val subtablaSalidaResp = Cell()
                .add(Paragraph("Resp:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)

            subtablaSalida.addCell(subtablaSalidaFecha)
            subtablaSalida.addCell(Cell(1, 2).add(Paragraph(LocalDate.now().toString()).setFontSize(fontSize)))
            subtablaSalida.addCell(subtablaSalidaTemp)
            subtablaSalida.addCell(Cell(1, 2).add(Paragraph("").setFontSize(fontSize)))
            subtablaSalida.addCell(subtablaSalidaResp)
            subtablaSalida.addCell(Cell(1, 2).add(Paragraph(pdmDetallado.ingeniero_campo).setFontSize(fontSize)))
            subtablaSalida.addCell(Cell(1, 3).add(Paragraph().setFontSize(fontSize)))

            val subtablaEntrada = Table(UnitValue.createPercentArray(floatArrayOf(1f,1f, 1f, 1f, 1f,1f)))
            subtablaEntrada.setWidth(UnitValue.createPercentValue(100f))
            val subtablaEntradaFecha = Cell()
                .add(Paragraph("Fecha:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)
            val subtablaEntradaTemp = Cell()
                .add(Paragraph("Temp:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)

            val subtablaEntradaResp = Cell()
                .add(Paragraph("Resp:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)

            subtablaEntrada.addCell(subtablaEntradaFecha)
            subtablaEntrada.addCell(Cell(1, 2).add(Paragraph("").setFontSize(fontSize)))
            subtablaEntrada.addCell(subtablaEntradaTemp)
            subtablaEntrada.addCell(Cell(1, 2).add(Paragraph("").setFontSize(fontSize)))
            subtablaEntrada.addCell(subtablaEntradaResp)
            subtablaEntrada.addCell(Cell(1, 2).add(Paragraph("").setFontSize(fontSize)))
            subtablaEntrada.addCell(Cell(1, 3).add(Paragraph("").setFontSize(fontSize)))

            val datosPuntoCriticaCell = Cell(2, 2)
                .add(Paragraph("DATOS DE SALIDA:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor).setTextAlignment(TextAlignment.CENTER)
            val datosrecepcionPuntoCritico = Cell(2, 2)
                .add(Paragraph("DATOS DE RECEPCION:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor).setTextAlignment(TextAlignment.CENTER)
            val idCliente = Cell(2, 2)
                .add(Paragraph("ID CLIENTE:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor).setTextAlignment(TextAlignment.CENTER)


            puntoCriticoTable.addCell(datosPuntoCriticaCell)
            puntoCriticoTable.addCell(Cell(2,2).add(subtablaSalida).setBorder(Border.NO_BORDER))
            puntoCriticoTable.addCell(datosrecepcionPuntoCritico)
            puntoCriticoTable.addCell(Cell(2,2).add(subtablaEntrada).setBorder(Border.NO_BORDER))
            puntoCriticoTable.addCell(idCliente)
            //Añadir una celda vacia
            puntoCriticoTable.addCell(Cell(2,2).add(Paragraph("").setFontSize(fontSize)))

            val tablaPrincipal = Table(2)
            tablaPrincipal.setWidth(UnitValue.createPercentValue(100f))

            // Agregar las tablas a la tabla principal
            tablaPrincipal.addCell(Cell().add(firmaTable).setBorder(Border.NO_BORDER))
            tablaPrincipal.addCell(Cell().add(puntoCriticoTable).setBorder(Border.NO_BORDER))


            // Agregar tablas al documento
            document.add(tablaPrincipal)


            val tableFooter = Table(UnitValue.createPercentArray(floatArrayOf(1f, 0.5f, 1.5f, 0.5f,1.5f)))
            tableFooter.setWidth(UnitValue.createPercentValue(100f)).setHorizontalAlignment(HorizontalAlignment.CENTER).setBackgroundColor(headerColor).setBorder(Border.NO_BORDER)

            val fontSizeFooter = 6f
            tableFooter.addCell(Cell().add(Paragraph("recepcionlab.lesa@gmail.com").setFontColor(whiteColor).setFontSize(fontSizeFooter)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("998 310 8622").setFontColor(whiteColor).setFontSize(fontSizeFooter)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell(2,2).add(Paragraph("DOCUMENTO CONTROLADO\n Documento propiedad de Centro Integral en Servicios de Laboratorio de Agua y Alimentos S.A de C.V.\n No puede reproducirse en forma parcial o total, si nla previa autorizacion del Laboratorio").setFontColor(whiteColor).setFontSize(fontSizeFooter).setTextAlignment(TextAlignment.CENTER)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("F-ING-LAB-02/ VERSION 0").setFontColor(whiteColor).setFontSize(fontSizeFooter).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com").setFontColor(whiteColor).setFontSize(fontSizeFooter)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("998 310 8623").setFontColor(whiteColor).setFontSize(fontSizeFooter)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("A.FRANCISCO I. MADERO MZ 107 LT 12 Int: LOCAL 4 REGION 94. CP 7717. Benito Juarez, Q.roo").setFontColor(whiteColor).setFontSize(5f).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))

            document.add(tableFooter)


            document.close()

            Toast.makeText(this, "PDF saved at $pdfPath/Muestras-Folio-${binding.tvFolio.text}.pdf", Toast.LENGTH_LONG).show()

            // Enviar el PDF por correo electrónico
            // SendEmailWorker(emailAddress, file).execute()
            // SendEmailTask(emailAddress, file).execute()



        } catch (e: Exception) {
            Toast.makeText(this, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("Error pdf:", e.toString())
        }

    }

    private fun savePdfExtra(emailAddress: String) {

        val pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val file = File(pdfPath, "Muestras-Folio-${binding.tvFolio.text}E.pdf")

        try {
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)

            val document = Document(pdfDocument, PageSize.A4.rotate())

            val footerHandler = FooterEventHandler(document)
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler)


            val inputStream = applicationContext.resources.openRawResource(R.raw.logorectangulartrans)
            val byteArrayOutputStream = ByteArrayOutputStream()
            var nextByte = inputStream.read()
            while (nextByte != -1) {
                byteArrayOutputStream.write(nextByte)
                nextByte = inputStream.read()
            }

            val imageData = byteArrayOutputStream.toByteArray()
            //--handler


            val image = Image(ImageDataFactory.create(imageData))
            image.scaleToFit(125f, 90f)
            //document.add(image)

            // Colores
            // Crear colores para la tabla
            // Crear colores para la tabla
            // Crear colores para la tabla
            val headerColor = DeviceCmyk(99, 38, 0, 67)
            val subHeaderColor = DeviceRgb(153, 204, 255)
            val whiteColor = DeviceRgb(255, 255, 255)
            val fontSize = 8f // Tamaño de fuente más pequeño

            // Crear tabla principal (2 columnas)
            val mainTable = Table(UnitValue.createPercentArray(floatArrayOf(5f, 1f))).useAllAvailableWidth().setBorder(Border.NO_BORDER)

            // Tabla de encabezado (3 columnas)
            val tableEncabezado = Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f))).useAllAvailableWidth().setBorder(Border.NO_BORDER)

            // Encabezado principal
            val mainHeaderCell = Cell(1, 2)
                .add(Paragraph("F-LAB 83. SOLICITUD DE SERVICIO DE ANÁLISIS DE AGUA Y ALIMENTOS").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor).setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            tableEncabezado.addCell(mainHeaderCell)

            // Sub-encabezado
            val subHeaderCell = Cell(1, 2)
                .add(Paragraph("Servicios que generan valor").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            tableEncabezado.addCell(subHeaderCell)

            // Crear subtabla para "DATOS DE SOLICITUD" (2 columnas)
            val datosSolicitudTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
            datosSolicitudTable.setWidth(UnitValue.createPercentValue(100f))
            datosSolicitudTable.setHeight(UnitValue.createPercentValue(100f))

            datosSolicitudTable.addCell(Cell().add(Paragraph("AÑO:").setFontSize(fontSize)).setFontColor(whiteColor).setBackgroundColor(headerColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(LocalDate.now().year.toString()).setFontSize(fontSize))).setBackgroundColor(whiteColor)

            datosSolicitudTable.addCell(Cell().add(Paragraph("MES:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(String.format("%02d", LocalDate.now().monthValue)).setFontSize(fontSize).setBold())).setBackgroundColor(whiteColor)

            datosSolicitudTable.addCell(Cell().add(Paragraph("DÍA:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(String.format("%02d", LocalDate.now().dayOfMonth)).setFontSize(fontSize).setBold())).setBackgroundColor(whiteColor)

            datosSolicitudTable.addCell(Cell().add(Paragraph("FOLIO:").setFontSize(fontSize).setFontColor(whiteColor)).setBackgroundColor(headerColor))
            datosSolicitudTable.addCell(Cell().add(Paragraph(binding.tvFolio.text.toString()+"E").setFontSize(fontSize))).setBackgroundColor(whiteColor)

            // Crear subtabla para "DATOS DE QUIEN SOLICITA LOS ANÁLISIS" (4 columnas)
            val datosSolicitanteTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f))).useAllAvailableWidth()

            datosSolicitanteTable.addCell(Cell().add(Paragraph("NOMBRE:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell(1, 3).add(Paragraph(clientePdm?.nombre_empresa).setFontSize(fontSize)))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("DIRECCIÓN:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell(1, 3).add(Paragraph(clientePdm?.direccion).setFontSize(fontSize)))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("ATENCIÓN A:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell().add(Paragraph(clientePdm?.atencion).setFontSize(fontSize)))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("TELÉFONO:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell().add(Paragraph(clientePdm?.telefono).setFontSize(fontSize)))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("PUESTO:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell().add(Paragraph(clientePdm?.puesto).setFontSize(fontSize)))

            datosSolicitanteTable.addCell(Cell().add(Paragraph("CORREO:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor))
            datosSolicitanteTable.addCell(Cell(1, 3).add(Paragraph(clientePdm?.correo).setFontSize(fontSize)))

            // Agregar sub-tablas a la tabla de encabezado en la misma fila
            tableEncabezado.addCell(Cell(1, 1).add(Paragraph("DATOS DE SOLICITUD").setFontColor(whiteColor)).setBackgroundColor(headerColor).setFontSize(fontSize))
            tableEncabezado.addCell(Cell(1, 2).add(Paragraph("DATOS DE QUIEN SOLICITA LOS ANÁLISIS").setFontColor(whiteColor)).setBackgroundColor(headerColor).setFontSize(fontSize))

            tableEncabezado.addCell(Cell().add(datosSolicitudTable)).setBorder(Border.NO_BORDER)
            tableEncabezado.addCell(Cell(1, 2).add(datosSolicitanteTable)).setBorder(Border.NO_BORDER)

            // Agregar la tabla de encabezado y el logo a la tabla principal
            mainTable.addCell(Cell().add(tableEncabezado).setBorder(Border.NO_BORDER)).setBorder(Border.NO_BORDER)
            mainTable.addCell(Cell().add(image).setVerticalAlignment(VerticalAlignment.MIDDLE).setHorizontalAlignment(HorizontalAlignment.RIGHT).setBorder(Border.NO_BORDER)).setBorder(Border.NO_BORDER)

            // Agregar la tabla principal al documento
            document.add(mainTable)

//            val logoPath = "res/raw/logorectangulartrans.png" // Ruta a tu imagen
//            val img = Image(ImageDataFactory.create(logoPath))
//            document.add(img)
//            document.add(Paragraph("Muestras Realizadas"))

            // Crear la tabla
            val table = Table(floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)).setMarginTop(10f).useAllAvailableWidth()
//            table.setWidth(UnitValue.createPercentValue(100f))

            // Agregar encabezados de celda
            val tabeadercell = Cell(1, 7)
                .add(Paragraph("Datos de las muestras colectadas").setFontColor(whiteColor).setFontSize(10f))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            table.addHeaderCell(tabeadercell)

            val estutablcell = Cell(1, 2)
                .add(Paragraph("Estudios a realizar").setFontColor(whiteColor).setFontSize(10f))
                .setBackgroundColor(DeviceRgb(22, 127, 165))
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

            addTableHeader(table)

            // Agregar filas de datos
            for (muestra in muestrasExtra) {
                addTableRow(table, muestra)
            }

            // Configurar el tamaño de fuente para las celdas
            table.setFontSize(7f)
            document.add(table)
            val paragrafoNomenclaturas = Paragraph("MA=Mesofilicos Aerobios - CT=Coliformes Totales - MH=Mohos - LV=Levaduras - CF=Coliformes Fecales - EC=Escherichia Coli - SA=Staphylococcus Aureus - SS=Salmonella spp - LM=Listeria Monocytogenes Vc=Vibrio Cholerae spp. - VP=Vibrio Parahemolitico spp. - PS=Pseudomona - CP = Clostridium Perfringens = BC=Bacillus Cereus - LP=Legionella spp. -ACA= Acanthamoeba spp. - NAE=Naegleria spp. - EFC=Enterococcus Fecales. - GL=Giardia Lamblia. - CLL=Cloro libre. - CCT= Cloro total.-PH=Potencial de Hidrógeno.-Crnas=Cloraminas,DUR=Dureza.- Alk=Alcalinidad. - SDT=Sólidos Disueltos Totales. - CE=Conductividad Electrica. - TUR=Turbidez ").setFontColor(DeviceRgb(1,1,1)).setFontSize(5f)

            document.add(paragrafoNomenclaturas)

            var signatureViewUno = binding.signatureViewUno
            val signatureBitmap = signatureViewUno.getSignatureBitmap()
            val stream = ByteArrayOutputStream()
            signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            // Crear la imagen de iTextPDF a partir del array de bytes
            val imagedata = ImageDataFactory.create(byteArray)
            val signatureImageUno = Image(imagedata).scaleToFit(100f, 100f)

            var signatureViewDos = binding.signatureViewDos
            val signatureBitmapDos = signatureViewDos.getSignatureBitmap()
            val streamDos = ByteArrayOutputStream()
            signatureBitmapDos.compress(Bitmap.CompressFormat.PNG, 100, streamDos)
            val byteArrayDos = streamDos.toByteArray()

            // Crear la imagen de iTextPDF a partir del array de bytes
            val imagedataDos = ImageDataFactory.create(byteArrayDos)
            val signatureImageDos = Image(imagedataDos).scaleToFit(100f, 100f)



            // Crear tabla para la sección de firmas
            val firmaTableExtra = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f)))
            firmaTableExtra.setWidth(UnitValue.createPercentValue(100f)).setHorizontalAlignment(HorizontalAlignment.CENTER)

// Encabezado
            val headerAutorizaCell = Cell(1, 3)
                .add(Paragraph("QUIÉN AUTORIZA ANÁLISIS DE LAS MUESTRAS").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            val headerFirmaCell1 = Cell()
                .add(Paragraph("FIRMA").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
//                .setBorder(Border.NO_BORDER)

            val headerTomaCell = Cell(1, 3)
                .add(Paragraph("QUIÉN TOMA LAS MUESTRAS").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
            val headerFirmaCell2 = Cell()
                .add(Paragraph("FIRMA").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)

            firmaTableExtra.addCell(headerAutorizaCell)
            firmaTableExtra.addCell(headerFirmaCell1)
//

// Datos de quien autoriza
            val autorizaNombreCell = Cell()
                .add(Paragraph("NOMBRE:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)
            val autorizaNombreValueCell = Cell(1,2)
                .add(Paragraph(binding.txtNombreAutoAnalisis.text.toString())).setFontSize(fontSize)
            val autorizaPuestoCell = Cell()
                .add(Paragraph("PUESTO:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)
            val autorizaPuestoValueCell = Cell(1,2)
                .add(Paragraph(binding.txtPuestoAutoAnalisis.text.toString())).setFontSize(fontSize)

            firmaTableExtra.addCell(autorizaNombreCell)
            firmaTableExtra.addCell(autorizaNombreValueCell)
            firmaTableExtra.addCell(Cell(2, 2).add(signatureImageUno))
            firmaTableExtra.addCell(autorizaPuestoCell)
            firmaTableExtra.addCell(autorizaPuestoValueCell)

// Datos de quien toma las muestras
            val tomaNombreCell = Cell()
                .add(Paragraph("NOMBRE:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)
            val tomaNombreValueCell = Cell(1, 2)
                .add(Paragraph(binding.txtNombreMuestreador.text.toString()).setFontSize(fontSize))

            val tomaPuestoCell = Cell()
                .add(Paragraph("PUESTO:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)
            val tomaPuestoValueCell = Cell(1, 2)
                .add(Paragraph("Ing. de campo").setFontSize(fontSize))

            firmaTableExtra.addCell(headerTomaCell)
            firmaTableExtra.addCell(headerFirmaCell2)
            firmaTableExtra.addCell(tomaNombreCell)
            firmaTableExtra.addCell(tomaNombreValueCell)
            firmaTableExtra.addCell(Cell(2, 2).add(signatureImageDos))
            firmaTableExtra.addCell(tomaPuestoCell)
            firmaTableExtra.addCell(tomaPuestoValueCell)


// Agregar tabla de firmas al documento

            val puntoCriticoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f)))
            puntoCriticoTable.setWidth(UnitValue.createPercentValue(80f)).setHorizontalAlignment(HorizontalAlignment.CENTER)

            val puntoCriticoHeader = Cell(1, 4)
                .add(Paragraph("PUNTO CRITICO PRE - ANALISIS").setFontColor(whiteColor).setFontSize(fontSize))
                .setBackgroundColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)



            puntoCriticoTable.addCell(puntoCriticoHeader)

            val subtablaSalida = Table(UnitValue.createPercentArray(floatArrayOf(1f,1f, 1f, 1f, 1f,1f)))
            subtablaSalida.setWidth(UnitValue.createPercentValue(100f))
            val subtablaSalidaFecha = Cell()
                .add(Paragraph("Fecha:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)
            val subtablaSalidaTemp = Cell()
                .add(Paragraph("Temp:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)

            val subtablaSalidaResp = Cell()
                .add(Paragraph("Resp:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)

            subtablaSalida.addCell(subtablaSalidaFecha)
            subtablaSalida.addCell(Cell(1, 2).add(Paragraph(LocalDate.now().toString()).setFontSize(fontSize)))
            subtablaSalida.addCell(subtablaSalidaTemp)
            subtablaSalida.addCell(Cell(1, 2).add(Paragraph("").setFontSize(fontSize)))
            subtablaSalida.addCell(subtablaSalidaResp)
            subtablaSalida.addCell(Cell(1, 2).add(Paragraph(pdmDetallado.ingeniero_campo).setFontSize(fontSize)))
            subtablaSalida.addCell(Cell(1, 3).add(Paragraph().setFontSize(fontSize)))

            val subtablaEntrada = Table(UnitValue.createPercentArray(floatArrayOf(1f,1f, 1f, 1f, 1f,1f)))
            subtablaEntrada.setWidth(UnitValue.createPercentValue(100f))
            val subtablaEntradaFecha = Cell()
                .add(Paragraph("Fecha:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)
            val subtablaEntradaTemp = Cell()
                .add(Paragraph("Temp:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)

            val subtablaEntradaResp = Cell()
                .add(Paragraph("Resp:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor)

            subtablaEntrada.addCell(subtablaEntradaFecha)
            subtablaEntrada.addCell(Cell(1, 2).add(Paragraph("").setFontSize(fontSize)))
            subtablaEntrada.addCell(subtablaEntradaTemp)
            subtablaEntrada.addCell(Cell(1, 2).add(Paragraph("").setFontSize(fontSize)))
            subtablaEntrada.addCell(subtablaEntradaResp)
            subtablaEntrada.addCell(Cell(1, 2).add(Paragraph("").setFontSize(fontSize)))
            subtablaEntrada.addCell(Cell(1, 3).add(Paragraph("").setFontSize(fontSize)))

            val datosPuntoCriticaCell = Cell(2, 2)
                .add(Paragraph("DATOS DE SALIDA:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor).setTextAlignment(TextAlignment.CENTER)
            val datosrecepcionPuntoCritico = Cell(2, 2)
                .add(Paragraph("DATOS DE RECEPCION:").setFontSize(fontSize)).setBackgroundColor(headerColor).setFontColor(whiteColor).setTextAlignment(TextAlignment.CENTER)

            puntoCriticoTable.addCell(datosPuntoCriticaCell)
            puntoCriticoTable.addCell(Cell(2,2).add(subtablaSalida).setBorder(Border.NO_BORDER))
            puntoCriticoTable.addCell(datosrecepcionPuntoCritico)
            puntoCriticoTable.addCell(Cell(2,2).add(subtablaEntrada).setBorder(Border.NO_BORDER))

            val tablaPrincipal = Table(2)
            tablaPrincipal.setWidth(UnitValue.createPercentValue(100f))

            // Agregar las tablas a la tabla principal
            tablaPrincipal.addCell(Cell().add(firmaTableExtra).setBorder(Border.NO_BORDER))
            tablaPrincipal.addCell(Cell().add(puntoCriticoTable).setBorder(Border.NO_BORDER))



            // Agregar tablas al documento
            document.add(tablaPrincipal)


            val tableFooter = Table(UnitValue.createPercentArray(floatArrayOf(1f, 0.5f, 1.5f, 0.5f,1.5f)))
            tableFooter.setWidth(UnitValue.createPercentValue(100f)).setHorizontalAlignment(HorizontalAlignment.CENTER).setBackgroundColor(headerColor).setBorder(Border.NO_BORDER)

            val fontSizeFooter = 6f
            tableFooter.addCell(Cell().add(Paragraph("recepcionlab.lesa@gmail.com").setFontColor(whiteColor).setFontSize(fontSizeFooter)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("998 310 8622").setFontColor(whiteColor).setFontSize(fontSizeFooter)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell(2,2).add(Paragraph("DOCUMENTO CONTROLADO\n Documento propiedad de Centro Integral en Servicios de Laboratorio de Agua y Alimentos S.A de C.V.\n No puede reproducirse en forma parcial o total, si nla previa autorizacion del Laboratorio").setFontColor(whiteColor).setFontSize(fontSizeFooter).setTextAlignment(TextAlignment.CENTER)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("F-ING-LAB-02/ VERSION 0").setFontColor(whiteColor).setFontSize(fontSizeFooter).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("operacioneslab.lesa@gmail.com,recepcionlab.lesa@gmail.com,cuentasporcobrarlab.lesa@gmail.com").setFontColor(whiteColor).setFontSize(fontSizeFooter)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("998 310 8623").setFontColor(whiteColor).setFontSize(fontSizeFooter)).setBorder(Border.NO_BORDER))
            tableFooter.addCell(Cell().add(Paragraph("A.FRANCISCO I. MADERO MZ 107 LT 12 Int: LOCAL 4 REGION 94. CP 7717. Benito Juarez, Q.roo").setFontColor(whiteColor).setFontSize(5f).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))

            document.add(tableFooter)


            document.close()

            Toast.makeText(this, "PDF saved at $pdfPath/Muestras-Folio-${binding.tvFolio.text}.pdf", Toast.LENGTH_LONG).show()

            // Enviar el PDF por correo electrónico
            // SendEmailWorker(emailAddress, file).execute()
            // SendEmailTask(emailAddress, file).execute()



        } catch (e: Exception) {
            Toast.makeText(this, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("Error pdf:", e.toString())
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

    fun enqueueSendEmailTask(
        context: Context,
        emailAddress: String,
        filePath: String,
        folioExtra: Boolean
    ) {
        Log.d("SendEmailTask", "Enviando correo electrónico... ${filePath}")

        val mensaje: String = if (folioExtra) {
            """
        <p>- Servicios que genera valor -</p>
        <p><img src="https://grupolesaa.com.mx/img/logorectangulartrans.png" alt="Logo Lesaa" width="300"/></p>
        <p><strong> ¡Tenemos información para tí!, </strong> </p>
        <p>Buen dia</p>
        <p>Esperando que se encuentre bien el dia de hoy, le notificamos que ha recibido reporte de servicio correspondiente a la colecta de muestras
         del día <strong> ${LocalDate.now()} </strong> con No. de folio <strong> ${binding.tvFolio.text}E </strong> el cual está en proceso y garantizamos la terminación de este en tiempo y forma.</p>
        <p>Sin más por el momento, quedamos a sus órdenes.</p>
        <p>¡Tenga un excelente día!</p>
        """.trimIndent()
        } else {
            """
        <p>- Servicios que genera valor -</p>
        <a href ="grupolesaa.com.mx"><img src="https://grupolesaa.com.mx/img/logorectangulartrans.png" alt="Logo Lesaa" width="300"/> </a>
        <p><strong> ¡Tenemos información para tí!, </strong> </p>
        <p>Buen dia</p>
        <p>Esperando que se encuentre bien el dia de hoy, le notificamos que ha recibido reporte de servicio correspondiente a la colecta de muestras
         del día <strong> ${LocalDate.now()} </strong> con No. de folio <strong> ${binding.tvFolio.text} </strong> el cual está en proceso y garantizamos la terminación de este en tiempo y forma.</p>
        <p>Sin más por el momento, quedamos a sus órdenes.</p>
        <p>¡Tenga un excelente día!</p>
        """.trimIndent()
        }

        val data = Data.Builder()
            .putString("emailAddress", emailAddress)
            .putString("filePath", filePath)
            .putString("subject","Reporte de servicio GRUPO LESAA")
            .putString("messageText", mensaje)
            .putBoolean("isHtml", true) // Agregar flag para indicar que es HTML
            .build()

        val sendEmailWorkRequest = OneTimeWorkRequest.Builder(SendEmailWorker::class.java)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(sendEmailWorkRequest)
    }


    fun convertirAMuestraPdmExtra(muestras: List<Muestra>): List<Muestra_pdmExtra> {
        if (muestras.isNotEmpty()){
            val listaMuestrasPdmExtra = mutableListOf<Muestra_pdmExtra>()
            for (muestra in muestras) {
                val muestraPdm = Muestra_pdmExtra(
                    registro_muestra = muestra.registroMuestra,
                    folio_muestreo = binding.tvFolio.text.toString(),
                    fecha_muestreo = muestra.fechaMuestra,
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
                    estudio_id = muestra.idEstudio.toInt()
                )
                listaMuestrasPdmExtra.add(muestraPdm)
            }
            return listaMuestrasPdmExtra

        }else{
            Log.e("Muestra","Algo paso")
        }
        return emptyList()
    }


    fun convertirAMuestraPdm(muestras: List<Muestra>): List<Muestra_pdm> {


            val listaMuestrasPdm = mutableListOf<Muestra_pdm>()

            for (muestra in muestras) {
                val muestraPdm = Muestra_pdm(
                    registro_muestra = muestra.registroMuestra,
                    folio_muestreo = binding.tvFolio.text.toString(),
                    fecha_muestreo = muestra.fechaMuestra,
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
                    servicio_id = muestra.servicioId,
                    subtipo = muestra.subtipo
                )
                listaMuestrasPdm.add(muestraPdm)
            }
            return listaMuestrasPdm

    }
//a
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



    /*fun generatePdfFromHtml(html: String, outputPdfPath: String) {

        val outputStream = FileOutputStream(outputPdfPath)
        val writer = PdfWriter(outputStream)
        val pdfDocument = com.itextpdf.kernel.pdf.PdfDocument(writer)
        pdfDocument.defaultPageSize = PageSize.A4.rotate()

        val document = Document(pdfDocument)

        val fontProvider: FontProvider = DefaultFontProvider()
        val converterProperties = ConverterProperties()
        HtmlConverter.convertToPdf(html, pdfDocument, converterProperties)
        document.close()

    }

    fun createHtmlWithTable(context: Context, muestras: List<Muestra>): String? {
        // Cargar el HTML base desde el directorio raw
        val inputStream = context.resources.openRawResource(R.raw.template)

        val templateContent: String
        try {
            templateContent = inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        val document: JsoupDocument = Jsoup.parse(templateContent)

        // Encontrar el cuerpo de la tabla
        val tableBody: Element = document.getElementById("table-body")!!

        // Agregar filas a la tabla
        for (muestra in muestras) {
            val row = Element("tr")
            row.appendElement("td").text(muestra.numeroMuestra)
            row.appendElement("td").text(muestra.fechaMuestra)
            row.appendElement("td").text(muestra.registroMuestra)
            row.appendElement("td").text(muestra.nombreMuestra)
            row.appendElement("td").text(muestra.idLab)
            row.appendElement("td").text(muestra.cantidadAprox)
            row.appendElement("td").text(muestra.tempM)
            row.appendElement("td").text(muestra.lugarToma)
            row.appendElement("td").text(muestra.descripcionM)
            row.appendElement("td").text(muestra.emicro)
            row.appendElement("td").text(muestra.efisico)
            row.appendElement("td").text(muestra.observaciones)
            tableBody.appendChild(row)
        }

        val imageUrl = "https://grupolesaa.com.mx/imagenes/logorectangulartrans.png"

        val imgElement = document.createElement("img")
        imgElement.attr("src", imageUrl)
        imgElement.attr("width", "210")
        imgElement.attr("alt", "Logo del Laboratorio")

        val divElement = document.createElement("div")
        divElement.appendChild(imgElement)
        divElement.appendText("Laboratorio de Control Sanitario")

        val tableBodyImg: Element = document.getElementById("content-img")
        tableBodyImg.appendChild(divElement)


        return document.outerHtml()
    }

    fun actualizarTablaHtml(htmlContent: String, datos: Map<String, String>): String? {
        // Parsear el contenido HTML con Jsoup
        val doc: JsoupDocument = Jsoup.parse(htmlContent)
        println(htmlContent)

        // Actualizar los valores en las celdas específicas
        datos.forEach { (id, valor) ->
            val elemento = doc.getElementById(id)
            elemento?.text(valor)
        }

        // Convertir el documento modificado de vuelta a HTML
        val htmlModificado = doc.outerHtml()

        // Imprimir el HTML modificado (solo para verificar en consola)
        println(htmlModificado)

        return htmlModificado
    }
*/








}
