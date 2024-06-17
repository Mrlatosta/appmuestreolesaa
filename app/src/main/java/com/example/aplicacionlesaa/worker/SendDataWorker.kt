package com.example.aplicacionlesaa.worker

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
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

                for (i in 0 until muestraCount) {
                    val muestra = Muestra_pdm(
                        registro_muestra = inputData.getString("registro_muestra_$i") ?: "",
                        folio_muestreo = inputData.getString("folio_muestreo_$i") ?: "",
                        fecha_muestreo = inputData.getString("fecha_muestreo_$i") ?: "",
                        hora_muestreo = inputData.getString("hora_muestreo_$i") ?: "",
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
                        servicio_id = inputData.getInt("servicio_id_$i", 0)
                    )
                    muestraMutableList.add(muestra)
                }
                Log.e("MuestraMutableList", "Se intento enviar ${muestraMutableList.size} muestras")
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

            val call = RetrofitClient.instance.createMuestreo(muestra)
            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            applicationContext,
                            "Muestras enviadas con éxito",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Error al enviar muestras",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(
                        applicationContext,
                        "Error de red: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
}

