package com.example.aplicacionlesaa.worker

import RetrofitClient
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.aplicacionlesaa.R
import com.example.aplicacionlesaa.api.ApiService
import com.example.aplicacionlesaa.model.DatosFinalesFolioMuestreo
import com.example.aplicacionlesaa.model.FolioMuestreo
import com.example.aplicacionlesaa.model.Muestra_pdm
import com.example.aplicacionlesaa.utils.NetworkUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.time.LocalDate


class SendDataWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

        private lateinit var muestraMutableList: MutableList<Muestra_pdm>
        //Crear un late iniit var string llamado FolioMuestreoAVerificar
        private lateinit var FolioMuestreoAVerificar: String
        private lateinit var pdmString: String
        private lateinit var fechaDelMuestreo: String
        private lateinit var folioCliente: String


        override fun doWork(): Result {
            return if (NetworkUtils.isInternetAvailable(applicationContext)) {
                try {
                    val filePath = inputData.getString("filePath")
                    if (filePath.isNullOrEmpty()) {
                        Log.e("SendDataWorker", "❌ No se recibió filePath en inputData")
                        return Result.failure()
                    }

                    val file = File(filePath)
                    if (!file.exists()) {
                        Log.e("SendDataWorker", "❌ Archivo no encontrado en $filePath")
                        return Result.failure()
                    }

                    val muestrasJson = file.readText()
                    val jsonObject = JSONObject(muestrasJson)

                    val folio = jsonObject.optString("folio")
                    FolioMuestreoAVerificar = jsonObject.optString("folio")
                    pdmString = jsonObject.optString("planMuestreo")


                    val muestrasArray = jsonObject.getJSONArray("muestras")

                    //Obtener folio del cliente que esta en un arreglo
                    val clientePdmA = jsonObject.getJSONObject("clientePdm")
                    folioCliente = clientePdmA.getString("folio")


                    val type = object : TypeToken<List<Muestra_pdm>>() {}.type
                    val muestras: List<Muestra_pdm> = Gson().fromJson(muestrasArray.toString(), type)

                    // ✅ Validar que registro_muestra nunca sea null
                    muestras.forEach {
                        requireNotNull(it.registro_muestra) { "registro_muestra no puede ser null" }
                    }
                    val fechaHoy = ""
                    val apiMuestras = muestras.map { m ->
                        //
                        //Si es la primera muestra extrar su fecha extraer por su index
                            if (m.registro_muestra == FolioMuestreoAVerificar + "-1") {
                                fechaDelMuestreo = m.fecha_muestreo
                                }

                        m.copy(
                            registro_muestra = m.registro_muestra,
                            folio_muestreo = folio,
                            fecha_muestreo = m.fecha_muestreo,
                            nombre_muestra = m.nombre_muestra,
                            id_lab = m.id_lab,
                            cantidad_aprox = m.cantidad_aprox,
                            temperatura = m.temperatura,
                            lugar_toma = m.lugar_toma,
                            descripcion_toma = m.descripcion_toma,
                            e_micro = m.e_micro,
                            e_fisico = m.e_fisico,
                            observaciones = m.observaciones,
                            folio_pdm = m.folio_pdm,
                            servicio_id = m.servicio_id,
                            estatus = "Pendiente",
                            subtipo = m.subtipo
                        )
                    }

                    Log.e("SendDataWorker", "📦 Preparadas ${apiMuestras.size} muestras para enviar")
                    Log.e("Las muestras 2vez son:","$apiMuestras" )
                    sendDataToApi(apiMuestras)

                    Result.success()

                } catch (e: Exception) {
                    Log.e("SendDataWorker", "⚠️ Error al enviar muestras: ${e.message}", e)
                    Result.retry()
                }
            } else {
                Log.e("SendDataWorker", "🌐 No hay conexión a internet, reintentando...")
                Result.retry()
            }
        }





        // ✅ Función corregida para Retrofit con List<Muestra_pdm>
        private fun sendDataToApi(muestras: List<Muestra_pdm>) {
            try {
                Log.e("SendDataWorker", "Enviando ${muestras.size} muestras en un solo paquete")

                val call = RetrofitClient.instance.createMuestrasBulk(muestras)
                val response = call.execute() // llamada síncrona

                if (response.isSuccessful) {
                    Log.e("SendDataWorker", "Muestras enviadas correctamente (${muestras.size})")

                    if (hasNotificationPermission()) {
                        showNotification("Muestras enviadas", "Se enviaron ${muestras.size} muestras correctamente.")
                    }

                    // Actualizar servicio
                    val restarServicioRequest = ApiService.RestarServicioRequest(cantidad = muestras.size)
                    val callUpdate = RetrofitClient.instance.restarServicio(muestras[0].servicio_id!!, restarServicioRequest)
                    val respUpdate = callUpdate.execute()
                    if (respUpdate.isSuccessful) {
                        Log.e("SendDataWorker", "Cantidad actualizada correctamente.")
                    } else {
                        Log.e("SendDataWorker", "Error al actualizar cantidad.")
                    }

                    sendDatosFaltantesToApi()

                } else {
                    Log.e("SendDataWorker", "Error al enviar muestras. Código: ${response.code()}")
                    if (hasNotificationPermission()) {
                        showNotification("Error al enviar", "No se pudieron enviar las muestras (${response.code()})")
                    }
                }

            } catch (e: Exception) {
                Log.e("SendDataWorker", "Error en envío bulk: ${e.message}", e)
                if (hasNotificationPermission()) {
                    showNotification("Error crítico", "Fallo al enviar muestras: ${e.message}")
                }
            }
        }




    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "MuestraNotificationChannel"
        val channelName = "Muestra Notification"
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)  // Asegúrate de tener un icono en tu drawable
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                return
            }
            notify(1, notificationBuilder.build())
        }

    }




        private fun sendDatosFaltantesToApi(): Result {

            return try {

                val nombreAutoAnalisis = inputData.getString("nombreAutoAnalisis") ?: ""
                val puestoAutoAnalisis = inputData.getString("puestoAutoAnalisis") ?: ""
                val nombreMuestreador = inputData.getString("nombreMuestreador") ?: ""
                val puestoMuestreador = inputData.getString("puestoMuestreador") ?: ""
                val folioText = FolioMuestreoAVerificar

                val datos = DatosFinalesFolioMuestreo(
                    nombreAutoAnalisis,
                    puestoAutoAnalisis,
                    nombreMuestreador,
                    puestoMuestreador
                )

                // 🔎 1️⃣ Verificar si existe
                Log.e("FolioText pa worker", folioText)
                val verificarResponse = RetrofitClient.instance
                    .verificarFolio(FolioMuestreoAVerificar)
                    .execute()

                if (verificarResponse.isSuccessful) {

                    // ✅ Existe → completar
                    val completarResponse = RetrofitClient.instance
                        .completarFolio(folioText, datos)
                        .execute()

                    if (completarResponse.isSuccessful) {
                        Result.success()
                    } else {
                        Result.retry()
                    }

                } else if (verificarResponse.code() == 404) {

                    // ❌ No existe → crear
                    val nuevoFolio = FolioMuestreo(
                        folio = folioText,
                        fecha = fechaDelMuestreo,
                        folio_cliente = folioCliente,
                        folio_pdm = pdmString
                    )

                    val crearResponse = RetrofitClient.instance
                        .createFolioMuestreo(nuevoFolio)
                        .execute()

                    if (crearResponse.isSuccessful) {

                        // 🔥 Luego completar
                        val completarResponse = RetrofitClient.instance
                            .completarFolio(folioText, datos)
                            .execute()

                        if (completarResponse.isSuccessful) {
                            Result.success()
                        } else {
                            Result.retry()
                        }

                    } else {
                        Result.retry()
                    }

                } else {
                    Result.failure()
                }

            } catch (e: Exception) {
                Log.e("SendDataWorker", "Error enviando datos", e)
                Result.retry()
            }
        }


}

