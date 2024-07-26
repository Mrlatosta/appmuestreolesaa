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
import com.example.aplicacionlesaa.model.Muestra_pdm
import com.example.aplicacionlesaa.utils.NetworkUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SendDataWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private lateinit var muestraMutableList: MutableList<Muestra_pdm>

    override fun doWork(): Result {
        return if (NetworkUtils.isInternetAvailable(applicationContext)) {
            try {
                // Obtener la lista de Muestras desde inputData
                muestraMutableList = mutableListOf()

                val muestraCount = inputData.getInt("muestra_count", 0)
                var contador = 0

                for (i in 0 until muestraCount) {
                    val muestra = Muestra_pdm(
                        registro_muestra = inputData.getString("registro_muestra_$i") ?: "",
                        folio_muestreo = inputData.getString("folio_muestreo_$i") ?: "",
                        fecha_muestreo = inputData.getString("fecha_muestreo_$i") ?: "",
                        nombre_muestra = inputData.getString("nombre_muestra_$i") ?: "",
                        id_lab = inputData.getString("id_lab_$i") ?: "",
                        cantidad_aprox = inputData.getString("cantidad_aprox_$i") ?: "",
                        temperatura = inputData.getString("temperatura_$i") ?: "",
                        lugar_toma = inputData.getString("lugar_toma_$i") ?: "",
                        descripcion_toma = inputData.getString("descripcion_toma_$i") ?: "",
                        e_micro = inputData.getString("e_micro_$i") ?: "",
                        e_fisico = inputData.getString("e_fisico_$i") ?: "",
                        observaciones = inputData.getString("observaciones_$i") ?: "",
                        folio_pdm = inputData.getString("folio_pdm_$i") ?: "",
                        servicio_id = inputData.getString("servicio_id_$i") ?: "",
                        estatus = "Pendiente",
                    )
                    muestraMutableList.add(muestra)
                    contador++
                }
                Log.e("MuestraMutableList", "Se intento enviar ${muestraMutableList.size} muestras")
                Log.e("Contador:","El contador es $contador")
                sendDataToApi(muestraMutableList)
                Result.success()
            } catch (e: Exception) {
                Log.e("SendDataWorker", "Error al enviar muestras: ${e.message}")
                Result.retry()
            }
        } else {
            Log.e("SendDataWorker", "No hay conexión a internet")
            Result.retry()
        }
    }

    private fun sendDataToApi(muestras: List<Muestra_pdm>) {
        for (muestra in muestras) {
            Log.e("Muestra es:", muestra.toString())

            val callCreateMuestreo = RetrofitClient.instance.createMuestreo(muestra)
            callCreateMuestreo.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Muestra enviada con éxito", Toast.LENGTH_SHORT).show()
                        if (hasNotificationPermission()) {
                            showNotification(
                                "Muestras enviadas",
                                "Las muestras se han enviado con éxito."
                            )
                        } else {
                            Log.e("SendDataWorker", "Permiso de notificaciones no concedido.")
                        }

                        // Preparar la solicitud de actualización
                        val restarServicioRequest = ApiService.RestarServicioRequest(cantidad = 1)

                        // Actualizar la cantidad del servicio
                        val callUpdateServicio = RetrofitClient.instance.restarServicio(muestra.servicio_id, restarServicioRequest)
                        callUpdateServicio.enqueue(object : Callback<Void> {
                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                if (response.isSuccessful) {
                                    if (hasNotificationPermission()) {
                                        showNotification(
                                            "Cantidad Actualizada",
                                            "Las cantidad de muestras se han actualizado con exito."
                                        )
                                    } else {
                                        Log.e("SendDataWorker", "Permiso de notificaciones no concedido.")
                                    }

                                    Toast.makeText(applicationContext, "Cantidad actualizada con éxito", Toast.LENGTH_SHORT).show()
                                    sendDatosFaltantesToApi()
                                } else {
                                    if (hasNotificationPermission()) {
                                        showNotification(
                                            "Cantidad NO Actualizada",
                                            "Las cantidad de muestras no se ha actualizado, error."
                                        )
                                    } else {
                                        Log.e("SendDataWorker", "Permiso de notificaciones no concedido.")
                                    }
                                    Toast.makeText(applicationContext, "Error al actualizar cantidad", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                Toast.makeText(applicationContext, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        if (hasNotificationPermission()) {
                            showNotification(
                                "Muestras no enviadas",
                                "Las muestras no se han podido enviar a la base de datos, ha ocurrido un error."
                            )
                        } else {
                            Log.e("SendDataWorker", "Permiso de notificaciones no concedido.")
                        }
                        Toast.makeText(applicationContext, "Error al enviar muestra", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(applicationContext, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
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

    private fun sendDatosFaltantesToApi() {
        val nombreAutoAnalisis = inputData.getString("nombreAutoAnalisis") ?: ""
        val puestoAutoAnalisis = inputData.getString("puestoAutoAnalisis") ?: ""
        val nombreMuestreador = inputData.getString("nombreMuestreador") ?: ""
        val puestoMuestreador = inputData.getString("puestoMuestreador") ?: ""
        val folioText = inputData.getString("folioText") ?: ""

        val datos = DatosFinalesFolioMuestreo(
            nombreAutoAnalisis,
            puestoAutoAnalisis,
            nombreMuestreador,
            puestoMuestreador
        )
        val callDatosFaltantes = RetrofitClient.instance.completarFolio(folioText, datos)
        callDatosFaltantes.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "Folio completado con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "Error al completar folio", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(applicationContext, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


}

