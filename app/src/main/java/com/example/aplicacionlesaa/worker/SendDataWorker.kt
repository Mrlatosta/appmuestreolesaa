package com.example.aplicacionlesaa.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.aplicacionlesaa.model.FolioMuestreo
import com.example.aplicacionlesaa.utils.NetworkUtils

class SendDataWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        return if (NetworkUtils.isInternetAvailable(applicationContext)) {
            try {
                val folioMuestreo = FolioMuestreo(
                    folio = inputData.getString("folio") ?: "",
                    fecha = inputData.getString("fecha") ?: "",
                    folio_cliente = inputData.getString("folio_cliente") ?: "",
                    folio_pdm = inputData.getString("folio_pdm") ?: ""
                )
                sendDataToApi(folioMuestreo)
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        } else {
            Result.retry()
        }
    }

    private fun sendDataToApi(folioMuestreo: FolioMuestreo) {
        val call = RetrofitClient.instance.createFolioMuestreo(folioMuestreo)
        val response = call.execute()
        if (!response.isSuccessful) {
            throw Exception("Error enviando datos")
        }
    }
}
