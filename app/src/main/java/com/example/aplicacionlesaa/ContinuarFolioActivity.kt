package com.example.aplicacionlesaa

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.adapter.MuestraResumenAdapter
import com.example.aplicacionlesaa.adapter.muestraAdapterActResumen
import com.example.aplicacionlesaa.adapter.servicioAdapter
import com.example.aplicacionlesaa.databinding.ActivityContinuarFolioBinding
import com.example.aplicacionlesaa.databinding.ActivityResendMuBinding
import com.example.aplicacionlesaa.model.Descripcion
import com.example.aplicacionlesaa.model.FolioMuestreo
import com.example.aplicacionlesaa.model.MuestraData
import com.example.aplicacionlesaa.model.Pdm
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDate

class ContinuarFolioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContinuarFolioBinding
    private lateinit var muestraAdapter: MuestraResumenAdapter

    // === DATA ORIGINAL ===
    private var muestraData: MuestraData? = null
    private var muestraMutableList: MutableList<Muestra> = mutableListOf()
    private var muestrasExtras: MutableList<Muestra> = mutableListOf()

    private val descripcionesList: MutableList<Descripcion> = mutableListOf()
    private val foliosHoy: MutableList<String> = mutableListOf()
    private val muestrasMap: MutableMap<String, MuestraData> = mutableMapOf()

    // === NUEVO (UI tipo SelePdm) ===
    private var selectedRow: TableRow? = null
    private var selectedMuestraData: MuestraData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityContinuarFolioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.recyclerMuestras.layoutManager = LinearLayoutManager(this)
        muestraAdapter = MuestraResumenAdapter(emptyList())
        binding.recyclerMuestras.adapter = muestraAdapter


        cargarArchivosAutomaticamente()
        cargarDescripciones()

        binding.btnContinuar.setOnClickListener {
            if (selectedMuestraData == null) {
                Toast.makeText(this, "Selecciona un folio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            muestraData = selectedMuestraData
            showConfirmationDialog()
        }
    }

    // =====================================================
    // ===============   CARGA DE DATOS   ==================
    // =====================================================

    private fun cargarDescripciones() {
        RetrofitClient.instance.getDescriptions()
            .enqueue(object : Callback<List<Descripcion>> {
                override fun onResponse(
                    call: Call<List<Descripcion>>,
                    response: Response<List<Descripcion>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { descripcionesList.addAll(it) }
                    }
                }

                override fun onFailure(call: Call<List<Descripcion>>, t: Throwable) {
                    Log.e("ContinuarFolio", "Error descripciones: ${t.message}")
                }
            })
    }

    private fun cargarArchivosAutomaticamente() {
        val directorio =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

        if (!directorio.exists()) return

        val fechaHoy = LocalDate.now().toString()
        val archivosHoy = directorio.listFiles { file ->
            file.extension == "json" && file.name.contains(fechaHoy)
        }

        archivosHoy?.forEach { archivo ->
            val uri = Uri.fromFile(archivo)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = InputStreamReader(inputStream)
                val type = object : TypeToken<MuestraData>() {}.type
                val data: MuestraData = Gson().fromJson(reader, type)

                foliosHoy.add(data.folio)
                muestrasMap[data.folio] = data
            }
        }

        cargarTablaFolios()
    }

    // =====================================================
    // ===============   TABLA (UI)   ======================
    // =====================================================

    private fun cargarTablaFolios() {
        val table = binding.tableFolios
        table.removeViews(1, table.childCount - 1)

        for (folio in foliosHoy) {
            val data = muestrasMap[folio] ?: continue

            val row = TableRow(this).apply {
                setOnClickListener {
                    selectedRow?.setBackgroundColor(Color.TRANSPARENT)
                    setBackgroundColor(Color.LTGRAY)
                    selectedRow = this
                    selectedMuestraData = data
                    binding.tvFolioSeleccionado.text = "Folio: ${data.folio}"
                    muestraAdapter.updateData(data.muestras)
                }
            }

            row.addView(createCell(data.folio))
            row.addView(createCell(data.clientePdm?.nombre_empresa ?: "-"))
            row.addView(createCell(data.planMuestreo))
            row.addView(createInfoCell(data))

            table.addView(row)
        }
    }

    private fun createCell(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setPadding(16, 16, 16, 16)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun createInfoCell(data: MuestraData): View =
        ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_info_details)
            setPadding(16, 16, 16, 16)
            setColorFilter(Color.parseColor("#536DFE"))
            setOnClickListener {
                showServiciosDialog(data)
            }
        }

    // =====================================================
    // ===============   DIALOG SERVICIOS   =================
    // =====================================================

    private fun showServiciosDialog(data: MuestraData) {
        val dialogView =
            layoutInflater.inflate(R.layout.dialog_servicio_list_selepdm, null)
        val recycler = dialogView.findViewById<RecyclerView>(R.id.recycler_view)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = servicioAdapter(
            servicioList = data.serviciosPdm.toMutableList(),
            onClickListener = {},
            onclickDelete = {}
        )

        val dialog = AlertDialog.Builder(this)
            .setTitle("Servicios del folio ${data.folio}")
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .create()

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // =====================================================
    // ===============   CONTINUAR FOLIO   =================
    // =====================================================

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirmación")
            .setMessage("¿Deseas continuar este folio?")
            .setPositiveButton("Sí") { _, _ -> performAction() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performAction() {
        val data = muestraData ?: return

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("plandemuestreo", data.planMuestreo)
            putParcelableArrayListExtra(
                "listaServicios",
                ArrayList(data.serviciosPdm)
            )
            putExtra("clientePdm", data.clientePdm)
            putExtra("folio", data.folio)
            putExtra("pdmDetallado", data.pdmDetallado)
            putParcelableArrayListExtra("muestraList", ArrayList(data.muestras))
            putParcelableArrayListExtra("muestraExtraList", ArrayList(data.muestrasExtra))
            putParcelableArrayListExtra("descripciones", ArrayList(descripcionesList))
            putExtra("tipomuestreo", "continuar")
        }

        startActivity(intent)
        finish()
    }
}
