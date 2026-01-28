package com.example.aplicacionlesaa

import RetrofitClient
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.adapter.servicioAdapter
import com.example.aplicacionlesaa.api.ApiService
import com.example.aplicacionlesaa.databinding.ActivitySelePdmBinding
import com.example.aplicacionlesaa.model.ClientePdm
import com.example.aplicacionlesaa.model.Descripcion
import com.example.aplicacionlesaa.model.FolioMuestreo
import com.example.aplicacionlesaa.model.Pdm
import com.example.aplicacionlesaa.model.Servicio
import com.example.aplicacionlesaa.model.UltimoFolio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate

class SelePdmActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelePdmBinding
    private val planesDetalladosList: MutableList<Pdm> = mutableListOf()
    private var servicioMutableList: MutableList<Servicio> = mutableListOf()
    private lateinit var adapter: servicioAdapter
    val apiService = RetrofitClient.instance

    // --- Variables para manejar la selección y los datos ---
    private var selectedRow: TableRow? = null
    private var selectedPdm: Pdm? = null
    private var siguienteFolioStr: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelePdmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- 1. CONFIGURAR VISTAS INICIALES ---
        setupFolio()
        initRecyclerView()

        // --- 2. CARGAR LOS PLANES DE MUESTREO EN LA TABLA ---
        loadAndDisplayPlans()

        // --- 3. CONFIGURAR EL BOTÓN DE CREAR ORDEN ---
        binding.btnCrearOrden.setOnClickListener {
            handleCreateOrder()
        }
    }

    private fun initRecyclerView() {
        adapter = servicioAdapter(
            servicioList = servicioMutableList,
            onClickListener = { servicio ->
                Toast.makeText(this, "Servicio: ${servicio.descripcion}", Toast.LENGTH_SHORT).show()
            },
            onclickDelete = { position ->
                Toast.makeText(this, "Acción de eliminar no implementada", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun crearFolioEnBackend(
        folioMuestreo: FolioMuestreo,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.createFolioMuestreo(folioMuestreo)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d("SelePdmActivity", "Folio creado correctamente")
                        onSuccess()
                    } else {
                        onError("Error al crear folio: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    onError("Error de red: ${t.message}")
                }
            })
    }


    private fun setupFolio() {
        apiService.getLastFolioMuestreo().enqueue(object : Callback<UltimoFolio> {
            override fun onResponse(call: Call<UltimoFolio>, response: Response<UltimoFolio>) {
                if (response.isSuccessful) {
                    val ultimoFolio = response.body()
                    val siguienteFolioNum = ultimoFolio?.folio?.toIntOrNull()?.plus(1) ?: 1
                    // Guardamos el folio para usarlo más tarde
                    siguienteFolioStr = String.format("%06d", siguienteFolioNum)

                    binding.folioContainer.removeAllViews()
                    val labelTextView = TextView(this@SelePdmActivity).apply {
                        text = "Folio No: "
                        textSize = 16f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(Color.BLACK)
                    }
                    val valueTextView = TextView(this@SelePdmActivity).apply {
                        text = siguienteFolioStr
                        textSize = 16f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(Color.RED)
                    }
                    binding.folioContainer.addView(labelTextView)
                    binding.folioContainer.addView(valueTextView)
                } else {
                    Toast.makeText(this@SelePdmActivity, "Error al obtener el último folio", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UltimoFolio>, t: Throwable) {
                Toast.makeText(this@SelePdmActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAndDisplayPlans() {
        Toast.makeText(this, "Cargando planes de muestreo...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getPlanesRecortado().execute()
                if (response.isSuccessful) {
                    val planes = response.body()
                    planes?.let {
                        planesDetalladosList.clear()
                        planesDetalladosList.addAll(it)
                        withContext(Dispatchers.Main) {
                            updateTableHeaders()
                            populatePlansTable(planesDetalladosList)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("SelePdmActivity", "Error al cargar planes: ${response.code()}")
                        Toast.makeText(this@SelePdmActivity, "Error al cargar planes", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("SelePdmActivity", "Fallo en la red: ${e.message}")
                    Toast.makeText(this@SelePdmActivity, "Fallo en la conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateTableHeaders() {
        val headerRow = binding.tablePlanesMuestreo.getChildAt(0) as TableRow
        headerRow.removeAllViews()
        headerRow.addView(createHeaderCell("Plan de Muestreo", 150))
        headerRow.addView(createHeaderCell("Nombre del cliente", 200))
        headerRow.addView(createHeaderCell("Fase", 150))
        headerRow.addView(createHeaderCell("Atención a", 150))
        headerRow.addView(createHeaderCell("Fecha y hora", 150))
        headerRow.addView(createHeaderCell("Nombre del muestreador", 150))
        headerRow.addView(createHeaderCell("Observaciones", 250))
        headerRow.addView(createHeaderCell("Info", 50, Gravity.CENTER))
    }

    private fun populatePlansTable(planes: List<Pdm>) {
        val tableLayout = binding.tablePlanesMuestreo
        val header = tableLayout.getChildAt(0)
        tableLayout.removeViews(1, tableLayout.childCount - 1)

        if (planes.isEmpty()) {
            val row = TableRow(this)
            val textView = TextView(this).apply {
                text = "No hay planes de muestreo para hoy."
                layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT).apply { span = 8 }
                gravity = Gravity.CENTER
                setPadding(8, 32, 8, 32)
            }
            row.addView(textView)
            tableLayout.addView(row)
            return
        }

        for (plan in planes) {
            val row = TableRow(this).apply {
                layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
                // --- MODIFICADO: Hacer la fila clickeable ---
                setOnClickListener {
                    selectedRow?.setBackgroundColor(Color.TRANSPARENT) // Deseleccionar la fila anterior
                    it.setBackgroundColor(Color.LTGRAY) // Seleccionar la nueva fila
                    selectedRow = it as TableRow
                    selectedPdm = plan
                }
            }

            row.addView(createTableCell(plan.nombre_pdm ?: "N/A"))
            row.addView(createTableCell(plan.nombre_empresa ?: "N/A"))
            row.addView(createTableCell("plan")) // Placeholder for fase

            row.addView(createTableCell(plan.pq_atendera ?: "N/A"))
            row.addView(createTableCell(plan.fecha_hora_cita ?: "N/A"))
            row.addView(createTableCell(plan.ingeniero_campo ?: "N/A"))
            row.addView(createTableCell(plan.observaciones ?: ""))
            row.addView(createInfoCell(plan))
            tableLayout.addView(row)
        }
    }



    // --- NUEVA FUNCIÓN: Lógica para el botón "Crear Orden" ---
    private fun handleCreateOrder() {
        val currentSelectedPdm = selectedPdm
        if (currentSelectedPdm == null) {
            Toast.makeText(this, "Selecciona un plan de muestreo", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Creando folio...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val servicesDeferred = async(Dispatchers.IO) {
                    apiService.getPlanServicesByName(currentSelectedPdm.nombre_pdm).execute()
                }
                val clientDeferred = async(Dispatchers.IO) {
                    apiService.getPlanClienteByPdmName(currentSelectedPdm.nombre_pdm).execute()
                }
                val descriptionsDeferred = async(Dispatchers.IO) {
                    apiService.getDescriptions().execute()
                }

                val servicesResponse = servicesDeferred.await()
                val clientResponse = clientDeferred.await()
                val descriptionsResponse = descriptionsDeferred.await()

                if (!servicesResponse.isSuccessful ||
                    !clientResponse.isSuccessful ||
                    !descriptionsResponse.isSuccessful
                ) {
                    Toast.makeText(this@SelePdmActivity, "Error al obtener datos del plan", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val listaServicios = servicesResponse.body()
                val clientePdm = clientResponse.body()
                val descripciones = descriptionsResponse.body()

                if (listaServicios.isNullOrEmpty() || clientePdm == null) {
                    Toast.makeText(this@SelePdmActivity, "Datos incompletos", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // 🔥 CREAR OBJETO FOLIO
                val folioMuestreo = FolioMuestreo(
                    folio = siguienteFolioStr,
                    fecha = LocalDate.now().toString(),
                    folio_cliente = clientePdm.folio,
                    folio_pdm = currentSelectedPdm.nombre_pdm
                )

                // 🔥 ENVIAR A BACKEND
                crearFolioEnBackend(
                    folioMuestreo,
                    onSuccess = {
                        val intent = Intent(this@SelePdmActivity, MainActivity::class.java).apply {
                            putExtra("plandemuestreo", currentSelectedPdm.nombre_pdm)
                            putParcelableArrayListExtra("listaServicios", ArrayList(listaServicios))
                            putExtra("clientePdm", clientePdm)
                            putExtra("folio", siguienteFolioStr)
                            putExtra("pdmDetallado", currentSelectedPdm)
                            putParcelableArrayListExtra("descripciones", ArrayList(descripciones))
                            putExtra("tipomuestreo", "nuevo")
                        }
                        startActivity(intent)
                    },
                    onError = { error ->
                        Toast.makeText(this@SelePdmActivity, error, Toast.LENGTH_LONG).show()
                    }
                )

            } catch (e: Exception) {
                Log.e("SelePdmActivity", "Error creando orden", e)
                Toast.makeText(this@SelePdmActivity, "Error inesperado", Toast.LENGTH_LONG).show()
            }
        }
    }



    private fun createHeaderCell(text: String, width: Int, gravity: Int = Gravity.START): TextView {
        return TextView(this).apply {
            this.text = text
            this.width = (width * resources.displayMetrics.density).toInt()
            setPadding(8, 8, 8, 8)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            this.gravity = gravity or Gravity.CENTER_VERTICAL
        }
    }

    private fun createTableCell(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setPadding(16, 16, 16, 16)
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun createInfoCell(plan: Pdm): View {
        val imageView = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_info_details)
            setPadding(16, 16, 16, 16)
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT)
            setColorFilter(Color.parseColor("#536DFE"))
            setOnClickListener {
                showServicesDialog(plan)
            }
        }
        return imageView
    }



    private fun showServicesDialog(plan: Pdm) {
        Toast.makeText(this, "Buscando servicios para ${plan.nombre_pdm}...", Toast.LENGTH_SHORT).show()
        apiService.getPlanServicesByName(plan.nombre_pdm).enqueue(object : Callback<List<Servicio>> {
            override fun onResponse(call: Call<List<Servicio>>, response: Response<List<Servicio>>) {
                if (response.isSuccessful) {
                    val services = response.body()
                    if (!services.isNullOrEmpty()) {
                        // Inflar el layout personalizado del diálogo
                        val dialogView = layoutInflater.inflate(R.layout.dialog_servicio_list_selepdm, null)
                        val servicesRecyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_view)

                        // Configurar el RecyclerView
                        servicesRecyclerView.layoutManager = LinearLayoutManager(this@SelePdmActivity)
                        val serviceAdapter = servicioAdapter(
                            servicioList = services,
                            onClickListener = { /* No action needed here */ },
                            onclickDelete = { /* No action needed here */ }
                        )
                        servicesRecyclerView.adapter = serviceAdapter

                        // Crear y mostrar el diálogo con la vista personalizada
                        val dialog = AlertDialog.Builder(this@SelePdmActivity)
                            .setTitle("Servicios para ${plan.nombre_pdm}")
                            .setView(dialogView) // Usar la vista inflada
                            .setNegativeButton("Cerrar") { dialog, _ -> dialog.dismiss() }
                            .create()

                        dialog.show()

                        // hacer el diaalogo mas ancho
                        dialog.window?.setLayout(
                            (resources.displayMetrics.widthPixels * 0.85).toInt(),
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )




                    } else {
                        Toast.makeText(this@SelePdmActivity, "No se encontraron servicios para este plan.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@SelePdmActivity, "Error al obtener los servicios: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<List<Servicio>>, t: Throwable) {
                Toast.makeText(this@SelePdmActivity, "Fallo en la conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }


    fun extractBeforeHyphen(input: String): String {
        val hyphenIndex = input.indexOf(" - ")
        return if (hyphenIndex != -1) {
            input.substring(0, hyphenIndex)
        } else {
            input
        }
    }
}