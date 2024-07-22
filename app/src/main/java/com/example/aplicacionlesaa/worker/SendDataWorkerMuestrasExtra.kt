package com.example.aplicacionlesaa.worker

import RetrofitClient
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aplicacionlesaa.R
import com.example.aplicacionlesaa.model.FolioMuestreo
import com.example.aplicacionlesaa.model.Muestra_pdmExtra
import com.example.aplicacionlesaa.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class SendDataWorkerMuestrasExtra(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!NetworkUtils.isInternetAvailable(applicationContext)) {
            Log.e("SendDataWorker", "No hay conexión a internet")
            return Result.retry()
        }

        return try {
            val folio = inputData.getString("folio") ?: ""
            val cliente = inputData.getString("cliente") ?: ""
            val folioPDM = inputData.getString("folioPDM") ?: ""
            val folioMuestreoExtra = FolioMuestreo(folio, LocalDate.now().toString(), cliente, folioPDM)

            val response = withContext(Dispatchers.IO) {
                RetrofitClient.instance.createFolioMuestreoExtra(folioMuestreoExtra).execute()
            }

            if (response.isSuccessful) {
                Log.d("sendDataToApiExtra", "Folio creado correctamente")
                val muestraCount = inputData.getInt("muestra_count", 0)
                val muestras = mutableListOf<Muestra_pdmExtra>()

                for (i in 0 until muestraCount) {
                    val muestra = Muestra_pdmExtra(
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
                        estudio_id = inputData.getInt("estudio_id_$i", 0),
                        estatus = "Pendiente"
                    )
                    muestras.add(muestra)
                    Log.e("SendatatoapiMuestras:", muestra.toString() )

                }
                Log.e("MuestraMutableList", "Se intentaron enviar ${muestras.size} muestras")

                sendMuestrasToApi(muestras)

                Result.success()
            } else {
                Log.e("sendDataToApiExtra", "Error al enviar datos, código: ${response.code()}, mensaje: ${response.message()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SendDataWorker", "Error al enviar muestras: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun sendMuestrasToApi(muestras: List<Muestra_pdmExtra>) {
        withContext(Dispatchers.IO) {
            muestras.forEach { muestra ->
                val response = RetrofitClient.instance.createMuestreoExtra(muestra).execute()
                if (response.isSuccessful) {
                    Log.d("sendDataToApi", "Muestra enviada con éxito")
                    if (hasNotificationPermission()) {
                        showNotification("Muestras enviadas", "Las muestras se han enviado con éxito.")
                    }
                } else {
                    if (hasNotificationPermission()) {
                        showNotification("Muestras no enviadas", "Las muestras no se han podido enviar a la base de datos.")
                    }
                    Log.e("SendDataWorkerMuestrasExtra", "Error al enviar muestra, código: ${response.code()}, mensaje: ${response.message()}")
                }
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "MuestraNotificationChannel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Muestra Notification", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        NotificationManagerCompat.from(applicationContext).notify(1, notificationBuilder.build())
    }
}


