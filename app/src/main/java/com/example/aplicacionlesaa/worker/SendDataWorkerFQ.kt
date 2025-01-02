package com.example.aplicacionlesaa.worker

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
import com.example.aplicacionlesaa.model.Muestra_pdm
import com.example.aplicacionlesaa.model.analisisFisico
import com.example.aplicacionlesaa.utils.NetworkUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigDecimal

class SendDataWorkerFQ(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams)  {
    private lateinit var fisicoQuimicosMutableList: MutableList<analisisFisico>

    override fun doWork(): Result {
        return if (NetworkUtils.isInternetAvailable(applicationContext)) {
            try {
                // Obtener la lista de Muestras desde inputData
                fisicoQuimicosMutableList = mutableListOf()

                val fisicoCount = inputData.getInt("fq_count", 0)
                var contador = 0
                for (i in 0 until fisicoCount){
                    val analisisfisico = analisisFisico(
                        registro_muestra = inputData.getString("registro_muestra_$i") ?: "",
                        nombre_muestra = inputData.getString("nombre_muestra_$i") ?: "",
                        hora_analisis = inputData.getString("hora_analisis_$i") ?: "",
                        temperatura = inputData.getString("temperatura_$i") ?: "",
                        ph = inputData.getString("ph_$i") ?: "",
                        clt = BigDecimal( inputData.getString("clt_$i") ?: "0"  ) ,
                        clr = BigDecimal(inputData.getString("clr_$i") ?: "0" ),
                        crnas = BigDecimal(inputData.getString("crnas_$i") ?: "0" ),
                        cya = BigDecimal(inputData.getString("cya_$i") ?: "0" ),
                        tur = BigDecimal(inputData.getString("tur_$i") ?: "0" )
                    )
                    fisicoQuimicosMutableList.add(analisisfisico)
                    contador++
                }
                Log.e("FisicoMutableList", "Se intento enviar ${fisicoQuimicosMutableList.size} Fisicoquimicos")
                Log.e("Contador:","El contador es $contador")
                sendDataToApi(fisicoQuimicosMutableList)
                Result.success()
            } catch (e: Exception) {
                Log.e("SendDataWorker", "Error al enviar Fisicoquimicos: ${e.message}")
                Result.retry()
            }
        } else {
            Log.e("SendDataWorker", "No hay conexión a internet")
            Result.retry()
        }
    }



    private fun sendDataToApi(fisicoquimicos: List<analisisFisico>) {
        for (fisico in fisicoquimicos) {
            Log.e("Fisicoquimico es:", fisico.toString())

            val callCreateFisico = RetrofitClient.instance.createFisicoquimicos(fisico)
            callCreateFisico.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Fisicoquimico enviada con éxito", Toast.LENGTH_SHORT).show()
                        if (hasNotificationPermission()) {
                            showNotification(
                                "Fisicoquimico enviadas",
                                "Los Fisicoquimicos se han enviado con éxito."
                            )
                        } else {
                            Log.e("SendDataWorker", "Permiso de notificaciones no concedido.")
                        }


                    } else {
                        if (hasNotificationPermission()) {
                            showNotification(
                                "Fisicoquimicos no enviadas",
                                "Los Fisicoquimicos no se han podido enviar a la base de datos, ha ocurrido un error."
                            )
                        } else {
                            Log.e("SendDataWorker", "Permiso de notificaciones no concedido.")
                        }
                        Toast.makeText(applicationContext, "Error al enviar Fisicoquimico", Toast.LENGTH_SHORT).show()
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


}