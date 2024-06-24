package com.example.aplicacionlesaa.worker

import RetrofitClient
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.aplicacionlesaa.model.DatosFinalesFolioMuestreo
import com.example.aplicacionlesaa.utils.NetworkUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SendDatosFaltantesWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {


    override fun doWork(): Result {
        return if (NetworkUtils.isInternetAvailable(applicationContext)) {
            try {
                // Obtener la lista de Muestras desde inputData
                sendDatosFaltantesToApi()
                Result.success()
            } catch (e: Exception) {
                Log.e("SendDatosFaltantesWorker", "Error al completar folio: ${e.message}")
                Result.retry()
            }
        } else {
            Log.e("SendDatosFaltantesWorker", "No hay conexión a internet")
            Result.retry()
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
