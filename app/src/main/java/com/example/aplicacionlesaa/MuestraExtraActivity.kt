package com.example.aplicacionlesaa

import DragManageAdapter
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.adapter.EstudiosAdapterInfo
import com.example.aplicacionlesaa.adapter.ServicioAdapterInfo
import com.example.aplicacionlesaa.adapter.muestraAdapter
import com.example.aplicacionlesaa.databinding.ActivityMuestraExtraBinding
import com.example.aplicacionlesaa.model.ClientePdm
import com.example.aplicacionlesaa.model.Descripcion
import com.example.aplicacionlesaa.model.Estudio
import com.example.aplicacionlesaa.model.MuestraData
import com.example.aplicacionlesaa.model.Pdm
import com.example.aplicacionlesaa.model.Servicio
import com.example.aplicacionlesaa.utils.OnItemMovedListener
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.ArrayList




class MuestraExtraActivity : AppCompatActivity(), OnItemMovedListener {

    private lateinit var binding: ActivityMuestraExtraBinding
    private lateinit var pdmDetallado: Pdm
    private var descripcionesList: MutableList<Descripcion> = mutableListOf()
    private var clientePdm: ClientePdm? = null
    private var folio : String? = null
    private var arregloDeNumeros: MutableList<Int> = mutableListOf()

    private var estudios = mapOf(
        1 to mapOf(
            "clasificacion" to "AGUA CRUDA",
            "clave_interna" to "CI-EST-001",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
        ),
        2 to mapOf(
            "clasificacion" to "AGUA DE POZO",
            "clave_interna" to "CI-EST-002",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
        ),
        3 to mapOf(
            "clasificacion" to "AGUA DE FUENTE ORNAMENTAL",
            "clave_interna" to "CI-EST-003",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
        ),
        4 to mapOf(
            "clasificacion" to "AGUA DE RED",
            "clave_interna" to "CI-EST-004",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
        ),
        5 to mapOf(
            "clasificacion" to "AGUA DE RIEGO",
            "clave_interna" to "CI-EST-005",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
        ),
        6 to mapOf(
            "clasificacion" to "HIELO",
            "clave_interna" to "CI-EST-006",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
        ),
        7 to mapOf(
            "clasificacion" to "AGUA DE OSMOSIS",
            "clave_interna" to "CI-EST-007",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
        ),
        8 to mapOf(
            "clasificacion" to "AGUA CARBOJET",
            "clave_interna" to "CI-EST-008",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
        ),
        9 to mapOf(
            "clasificacion" to "AGUA PURIFICADA",
            "clave_interna" to "CI-EST-009",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
        ),
        10 to mapOf(
            "clasificacion" to "AGUA ALCALINA",
            "clave_interna" to "CI-EST-010",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
        ),
        11 to mapOf(
            "clasificacion" to "ALIMENTOS COCIDOS",
            "clave_interna" to "CI-EST-011",
            "cantidad_toma" to "300gr",
            "norma" to "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
        ),
        12 to mapOf(
            "clasificacion" to "ALIMENTOS CRUDOS",
            "clave_interna" to "CI-EST-012",
            "cantidad_toma" to "300gr",
            "norma" to "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
        ),
        13 to mapOf(
            "clasificacion" to "SUPERFICIE VIVA",
            "clave_interna" to "CI-EST-013",
            "cantidad_toma" to "10ml",
            "norma" to "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
        ),
        14 to mapOf(
            "clasificacion" to "SUPERFICIE INERTE",
            "clave_interna" to "CI-EST-014",
            "cantidad_toma" to "10ml",
            "norma" to "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
        ),
        15 to mapOf(
            "clasificacion" to "POSTRES",
            "clave_interna" to "CI-EST-015",
            "cantidad_toma" to "300gr",
            "norma" to "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
        ),
        16 to mapOf(
            "clasificacion" to "POSTRES",
            "clave_interna" to "CI-EST-016",
            "cantidad_toma" to "300gr",
            "norma" to "NOM-247-SSA1-2008, Productos y servicios. Cereales y sus productos. Cereales, harinas de cereales, sémolas o semolinas. Alimentos a base de: cereales, semillas comestibles, de harinas, sémolas o semolinas o sus mezclas. Productos de panificación. Disposiciones y especificaciones sanitarias y nutrimentales. Métodos de prueba."
        ),
        17 to mapOf(
            "clasificacion" to "ALIMENTO A BASE LÁCTEA",
            "clave_interna" to "CI-EST-017",
            "cantidad_toma" to "300gr",
            "norma" to "NOM-243-SSA1-2010, Productos y servicios. Leche, fórmula láctea, producto lácteo combinado y derivados lácteos. Disposiciones y especificaciones sanitarias. Métodos de prueba."
        ),
        18 to mapOf(
            "clasificacion" to "PRODUCTOS DE LA PESCA FRESCOS,REFRIGERADOS,CONGELADOS Y PROCESADOS",
            "clave_interna" to "CI-EST-018",
            "cantidad_toma" to "300gr",
            "norma" to "NOM-242-SSA1-2009, Productos y servicios. Productos de la pesca frescos, refrigerados, congelados y procesados. Especificaciones sanitarias y métodos de prueba."
        ),
        19 to mapOf(
            "clasificacion" to "PRODUCTOS CARNICOS PROCESADOS",
            "clave_interna" to "CI-EST-019",
            "cantidad_toma" to "300gr",
            "norma" to "NOM-213-SSA1-2002, Productos y servicios. Productos cárnicos procesados. Especificaciones sanitarias."
        ),
        20 to mapOf(
            "clasificacion" to "AGUA DE ALBERCA (Microbiologicos)",
            "clave_interna" to "CI-EST-020",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio microbiológico)"
        ),
        21 to mapOf(
            "clasificacion" to "AGUA DE ALBERCA (Fisicoquimicos)",
            "clave_interna" to "CI-EST-021",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio fisicoquimico)"
        ),
        22 to mapOf(
            "clasificacion" to "AGUA DE JACUZZI (Microbiologicos)",
            "clave_interna" to "CI-EST-022",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio microbiológico)"
        ),
        23 to mapOf(
            "clasificacion" to "AGUA DE JACUZZI (Fisicoquimicos)",
            "clave_interna" to "CI-EST-023",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio fisicoquimico)"
        ),
        24 to mapOf(
            "clasificacion" to "AGUA RESIDUAL",
            "clave_interna" to "CI-EST-024",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-001-SEMARNAT-2021, Que establece los límites permisibles de contaminantes en las descargas de aguas residuales en cuerpos receptores propiedad de la nación."
        ),
        25 to mapOf(
            "clasificacion" to "AGUA RESIDUAL",
            "clave_interna" to "CI-EST-025",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-002-ECOL-1996, Que establece los límites máximos permisibles de contaminantes en las descargas de aguas residuales a los sistemas de alcantarillado urbano o municipal."
        ),
        26 to mapOf(
            "clasificacion" to "PTAR",
            "clave_interna" to "CI-EST-026",
            "cantidad_toma" to "NULL",
            "norma" to "NOM-001-SEMARNAT-2021, Que establece los límites permisibles de contaminantes en las descargas de aguas residuales en cuerpos receptores propiedad de la nación."
        ),
        27 to mapOf(
            "clasificacion" to "PTAR",
            "clave_interna" to "CI-EST-027",
            "cantidad_toma" to "NULL",
            "norma" to "NOM-002-ECOL-1996, Que establece los límites máximos permisibles de contaminantes en las descargas de aguas residuales a los sistemas de alcantarillado urbano o municipal."
        ),
        28 to mapOf(
            "clasificacion" to "LODO",
            "clave_interna" to "CI-EST-028",
            "cantidad_toma" to "NULL",
            "norma" to "NOM-004-SEMARNAT-2002, Protección ambiental.- Lodos y biosólidos.-Especificaciones y límites máximos permisibles de contaminantes para su aprovechamiento y disposición final."
        ),
        29 to mapOf(
            "clasificacion" to "LEGIONELLA",
            "clave_interna" to "CI-EST-029",
            "cantidad_toma" to "NULL",
            "norma" to "UNE 100030:2017 Prevención y control de la proliferación y diseminación de Legionella en instalaciones."
        ),
        30 to mapOf(
            "clasificacion" to "Ma",
            "clave_interna" to "CI-EST-030",
            "cantidad_toma" to "NULL",
            "norma" to "UNE 100030:2017 Prevención y control de la proliferación y diseminación de Mesofilicos Aerobios en instalaciones."
        ),
        31 to mapOf(
            "clasificacion" to "SALMONELLA",
            "clave_interna" to "CI-EST-031",
            "cantidad_toma" to "NULL",
            "norma" to "Perfil de estudio para determinación de Salmonella."
        ),
        32 to mapOf(
            "clasificacion" to "VIBRIO CHOLERAE",
            "clave_interna" to "CI-EST-032",
            "cantidad_toma" to "NULL",
            "norma" to "Perfil de estudio para determinación de Vibrio Cholerae."
        ),
        33 to mapOf(
            "clasificacion" to "HIERRO",
            "clave_interna" to "CI-EST-033",
            "cantidad_toma" to "NULL",
            "norma" to "Perfil de estudios para determinación de HIERRO."
        ),
        34 to mapOf(
            "clasificacion" to "E.COLI",
            "clave_interna" to "CI-EST-034",
            "cantidad_toma" to "NULL",
            "norma" to "Perfil de estudios para determinación de E.COLI."
        ),
        35 to mapOf(
            "clasificacion" to "STAPHYLOCOCCUS AUREUS",
            "clave_interna" to "CI-EST-035",
            "cantidad_toma" to "NULL",
            "norma" to "Perfil de estudios para determinación de STAPHYLOCOCCUS."
        ),
        36 to mapOf(
            "clasificacion" to "SERVICIO DE RECOLECCION DE MUESTRAS",
            "clave_interna" to "CI-EST-036",
            "cantidad_toma" to "NULL",
            "norma" to "N/A."
        ),
        37 to mapOf(
            "clasificacion" to "AGUA TRATADA",
            "clave_interna" to "CI-EST-037",
            "cantidad_toma" to "100ml",
            "norma" to "NOM-003-ECOL-1997, Que establece los límites máximos permisibles de contaminantes para las aguas residuales tratadas que se reusen en servicios al público."
        ),
        38 to mapOf(
            "clasificacion" to "PROTEINAS CRUDAS",
            "clave_interna" to "CI-EST-038",
            "cantidad_toma" to "300gr",
            "norma" to "NOM-213-SSA1-2018, Productos y servicios. Productos cárnicos procesados y los establecimientos dedicados a su proceso. Disposiciones y especificaciones sanitarias. Métodos de prueba.; NOM-242-SSA1-2009, Productos de la pesca frescos, refrigerados, congelados y procesados. Especificaciones sanitarias y métodos de prueba."
        )
    )
    private val estudiosList = mutableListOf<Estudio>(
        Estudio(
            1,
            "AGUA CRUDA",
            "CI-EST-001",
            "100ml",
            "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
        ),
        Estudio(
            2,
            "AGUA DE POZO",
            "CI-EST-002",
            "100ml",
            "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
        ),
        Estudio(
            3,
            "AGUA DE FUENTE ORNAMENTAL",
            "CI-EST-003",
            "100ml",
            "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
        ),
        Estudio(
            4,
            "AGUA DE RED",
            "CI-EST-004",
            "100ml",
            "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
        ),
        Estudio(
            5,
            "AGUA DE RIEGO",
            "CI-EST-005",
            "100ml",
            "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
        ),
        Estudio(
            6,
            "HIELO",
            "CI-EST-006",
            "100ml",
            "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
        ),
        Estudio(
            7,
            "AGUA DE OSMOSIS",
            "CI-EST-007",
            "100ml",
            "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
        ),
        Estudio(
            8,
            "AGUA CARBOJET",
            "CI-EST-008",
            "100ml",
            "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
        ),
        Estudio(
            9,
            "AGUA PURIFICADA",
            "CI-EST-009",
            "100ml",
            "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
        ),
        Estudio(
            10,
            "AGUA ALCALINA",
            "CI-EST-010",
            "100ml",
            "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
        ),
        Estudio(
            11,
            "ALIMENTOS COCIDOS",
            "CI-EST-011",
            "300gr",
            "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
        ),
        Estudio(
            12,
            "ALIMENTOS CRUDOS",
            "CI-EST-012",
            "300gr",
            "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
        ),
        Estudio(
            13,
            "SUPERFICIE VIVA",
            "CI-EST-013",
            "10ml",
            "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
        ),
        Estudio(
            14,
            "SUPERFICIE INERTE",
            "CI-EST-014",
            "10ml",
            "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
        ),
        Estudio(
            15,
            "POSTRES",
            "CI-EST-015",
            "300gr",
            "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
        ),
        Estudio(
            16,
            "POSTRES",
            "CI-EST-016",
            "300gr",
            "NOM-247-SSA1-2008, Productos y servicios. Cereales y sus productos. Cereales, harinas de cereales, sémolas o semolinas. Alimentos a base de: cereales, semillas comestibles, de harinas, sémolas o semolinas o sus mezclas. Productos de panificación. Disposiciones y especificaciones sanitarias y nutrimentales. Métodos de prueba."
        ),
        Estudio(
            17,
            "ALIMENTO A BASE LÁCTEA",
            "CI-EST-017",
            "300gr",
            "NOM-243-SSA1-2010, Productos y servicios. Leche, fórmula láctea, producto lácteo combinado y derivados lácteos. Disposiciones y especificaciones sanitarias. Métodos de prueba."
        ),
        Estudio(
            18,
            "PRODUCTOS DE LA PESCA FRESCOS,REFRIGERADOS,CONGELADOS Y PROCESADOS",
            "CI-EST-018",
            "300gr",
            "NOM-242-SSA1-2009, Productos y servicios. Productos de la pesca frescos, refrigerados, congelados y procesados. Especificaciones sanitarias y métodos de prueba."
        ),
        Estudio(
            19,
            "PRODUCTOS CARNICOS PROCESADOS",
            "CI-EST-019",
            "300gr",
            "NOM-213-SSA1-2002, Productos y servicios. Productos cárnicos procesados. Especificaciones sanitarias."
        ),
        Estudio(
            20,
            "AGUA DE ALBERCA (Microbiologicos)",
            "CI-EST-020",
            "100ml",
            "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio microbiológico)"
        ),
        Estudio(
            21,
            "AGUA DE ALBERCA (Fisicoquimicos)",
            "CI-EST-021",
            "100ml",
            "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio fisicoquimico)"
        ),
        Estudio(
            22,
            "AGUA DE JACUZZI (Microbiologicos)",
            "CI-EST-022",
            "100ml",
            "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio microbiológico)"
        ),
        Estudio(
            23,
            "AGUA DE JACUZZI (Fisicoquimicos)",
            "CI-EST-023",
            "100ml",
            "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio fisicoquimico)"
        ),
        Estudio(
            24,
            "AGUA RESIDUAL",
            "CI-EST-024",
            "100ml",
            "NOM-001-SEMARNAT-2021, Que establece los límites permisibles de contaminantes en las descargas de aguas residuales en cuerpos receptores propiedad de la nación."
        ),
        Estudio(
            25,
            "AGUA RESIDUAL",
            "CI-EST-025",
            "100ml",
            "NOM-002-ECOL-1996, Que establece los límites máximos permisibles de contaminantes en las descargas de aguas residuales a los sistemas de alcantarillado urbano o municipal."
        ),
        Estudio(
            26,
            "PTAR",
            "CI-EST-026",
            "NULL",
            "NOM-001-SEMARNAT-2021, Que establece los límites permisibles de contaminantes en las descargas de aguas residuales en cuerpos receptores propiedad de la nación."
        ),
        Estudio(
            27,
            "PTAR",
            "CI-EST-027",
            "NULL",
            "NOM-001-SEMARNAT-2021, Que establece los límites permisibles de contaminantes en las descargas de aguas residuales en cuerpos receptores propiedad de la nación."
        ),
        Estudio(
            28,
            "LODO",
            "CI-EST-028",
            "NULL",
            "NOM-004-SEMARNAT-2002, Protección ambiental.- Lodos y biosólidos.- Especificaciones y límites máximos permisibles de contaminantes para su aprovechamiento y disposición final."
        ),
        Estudio(
            29,
            "LEGIONELLA",
            "CI-EST-029",
            "NULL",
            "UNE 100030:2017 Prevención y control de la proliferación y diseminación de Legionella en instalaciones."
        ),
        Estudio(
            30,
            "Ma",
            "CI-EST-030",
            "NULL",
            "UNE 100030:2017 Prevención y control de la proliferación y diseminación de Mesofilicos Aerobios en instalaciones."
        ),
        Estudio(
            31,
            "SALMONELLA",
            "CI-EST-031",
            "NULL",
            "Perfil de estudio para determinación de Salmonella."
        ),
        Estudio(
            32,
            "VIBRIO CHOLERAE",
            "CI-EST-032",
            "NULL",
            "Perfil de estudio para determinación de Vibrio Cholerae."
        ),
        Estudio(
            33,
            "HIERRO",
            "CI-EST-033",
            "NULL",
            "Perfil de estudios para determinación de HIERRO."
        ),
        Estudio(
            34,
            "E.COLI",
            "CI-EST-034",
            "NULL",
            "Perfil de estudios para determinación de E.COLI."
        ),
        Estudio(
            35,
            "STAPHYLOCOCCUS AUREUS",
            "CI-EST-035",
            "NULL",
            "Perfil de estudios para determinación de STAPHYLOCOCCUS."
        ),
        Estudio(36, "SERVICIO DE RECOLECCION DE MUESTRAS", "CI-EST-036", "NULL", "N/A."),
        Estudio(
            37,
            "AGUA TRATADA",
            "CI-EST-037",
            "100ml",
            "NOM-003-ECOL-1997, Que establece los límites máximos permisibles de contaminantes para las aguas residuales tratadas que se reusen en servicios al público."
        ),
        Estudio(
            38,
            "PROTEINAS CRUDAS",
            "CI-EST-038",
            "300gr",
            "NOM-213-SSA1-2018, Productos y servicios. Productos cárnicos procesados y los establecimientos dedicados a su proceso. Disposiciones y especificaciones sanitarias. Métodos de prueba.; NOM-242-SSA1-2009, Productos de la pesca frescos, refrigerados, congelados y procesados. Especificaciones sanitarias y métodos de prueba."
        )
    )
//    private var estudios = mapOf(
//        1 to mapOf(
//            "clasificacion" to "AGUA CRUDA",
//            "clave_interna" to "CI-EST-001",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
//        ),
//        2 to mapOf(
//            "clasificacion" to "AGUA DE POZO",
//            "clave_interna" to "CI-EST-002",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
//        ),
//        3 to mapOf(
//            "clasificacion" to "AGUA DE FUENTE ORNAMENTAL",
//            "clave_interna" to "CI-EST-003",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
//        ),
//        4 to mapOf(
//            "clasificacion" to "AGUA DE RED",
//            "clave_interna" to "CI-EST-004",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
//        ),
//        5 to mapOf(
//            "clasificacion" to "AGUA DE RIEGO",
//            "clave_interna" to "CI-EST-005",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-127-SSA1-2021, Agua para uso y consumo humano. Límites permisibles de la calidad del agua."
//        ),
//        6 to mapOf(
//            "clasificacion" to "HIELO",
//            "clave_interna" to "CI-EST-006",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
//        ),
//        7 to mapOf(
//            "clasificacion" to "AGUA DE OSMOSIS",
//            "clave_interna" to "CI-EST-007",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
//        ),
//        8 to mapOf(
//            "clasificacion" to "AGUA CARBOJET",
//            "clave_interna" to "CI-EST-008",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
//        ),
//        9 to mapOf(
//            "clasificacion" to "AGUA PURIFICADA",
//            "clave_interna" to "CI-EST-009",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
//        ),
//        10 to mapOf(
//            "clasificacion" to "AGUA ALCALINA",
//            "clave_interna" to "CI-EST-010",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-201-SSA1-2015, Productos y servicios. Agua y hielo para consumo humano, envasados y a granel. Especificaciones sanitarias."
//        ),
//        11 to mapOf(
//            "clasificacion" to "ALIMENTOS COCIDOS",
//            "clave_interna" to "CI-EST-011",
//            "cantidad_toma" to "300gr",
//            "norma" to "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
//        ),
//        12 to mapOf(
//            "clasificacion" to "ALIMENTOS CRUDOS",
//            "clave_interna" to "CI-EST-012",
//            "cantidad_toma" to "300gr",
//            "norma" to "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
//        ),
//        13 to mapOf(
//            "clasificacion" to "SUPERFICIE VIVA",
//            "clave_interna" to "CI-EST-013",
//            "cantidad_toma" to "10ml",
//            "norma" to "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
//        ),
//        14 to mapOf(
//            "clasificacion" to "SUPERFICIE INERTE",
//            "clave_interna" to "CI-EST-014",
//            "cantidad_toma" to "10ml",
//            "norma" to "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
//        ),
//        15 to mapOf(
//            "clasificacion" to "POSTRES",
//            "clave_interna" to "CI-EST-015",
//            "cantidad_toma" to "300gr",
//            "norma" to "NOM-093-SSA1-1994, Bienes y servicios. Prácticas de higiene y sanidad en la preparación de alimentos que se ofrecen en establecimientos fijos."
//        ),
//        16 to mapOf(
//            "clasificacion" to "POSTRES",
//            "clave_interna" to "CI-EST-016",
//            "cantidad_toma" to "300gr",
//            "norma" to "NOM-247-SSA1-2008, Productos y servicios. Cereales y sus productos. Cereales, harinas de cereales, sémolas o semolinas. Alimentos a base de: cereales, semillas comestibles, de harinas, sémolas o semolinas o sus mezclas. Productos de panificación. Disposiciones y especificaciones sanitarias y nutrimentales. Métodos de prueba."
//        ),
//        17 to mapOf(
//            "clasificacion" to "ALIMENTO A BASE LÁCTEA",
//            "clave_interna" to "CI-EST-017",
//            "cantidad_toma" to "300gr",
//            "norma" to "NOM-243-SSA1-2010, Productos y servicios. Leche, fórmula láctea, producto lácteo combinado y derivados lácteos. Disposiciones y especificaciones sanitarias. Métodos de prueba."
//        ),
//        18 to mapOf(
//            "clasificacion" to "PRODUCTOS DE LA PESCA FRESCOS,REFRIGERADOS,CONGELADOS Y PROCESADOS",
//            "clave_interna" to "CI-EST-018",
//            "cantidad_toma" to "300gr",
//            "norma" to "NOM-242-SSA1-2009, Productos y servicios. Productos de la pesca frescos, refrigerados, congelados y procesados. Especificaciones sanitarias y métodos de prueba."
//        ),
//        19 to mapOf(
//            "clasificacion" to "PRODUCTOS CARNICOS PROCESADOS",
//            "clave_interna" to "CI-EST-019",
//            "cantidad_toma" to "300gr",
//            "norma" to "NOM-213-SSA1-2002, Productos y servicios. Productos cárnicos procesados. Especificaciones sanitarias."
//        ),
//        20 to mapOf(
//            "clasificacion" to "AGUA DE ALBERCA (Microbiologicos)",
//            "clave_interna" to "CI-EST-020",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio microbiológico)"
//        ),
//        21 to mapOf(
//            "clasificacion" to "AGUA DE ALBERCA (Fisicoquimicos)",
//            "clave_interna" to "CI-EST-021",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio fisicoquimico)"
//        ),
//        22 to mapOf(
//            "clasificacion" to "AGUA DE JACUZZI (Microbiologicos)",
//            "clave_interna" to "CI-EST-022",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio microbiológico)"
//        ),
//        23 to mapOf(
//            "clasificacion" to "AGUA DE JACUZZI (Fisicoquimicos)",
//            "clave_interna" to "CI-EST-023",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-245-SSA1-2010, Requisitos sanitarios y calidad del agua que deben cumplir las albercas. ( Estudio fisicoquimico)"
//        ),
//        24 to mapOf(
//            "clasificacion" to "AGUA RESIDUAL",
//            "clave_interna" to "CI-EST-024",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-001-SEMARNAT-2021, Que establece los límites permisibles de contaminantes en las descargas de aguas residuales en cuerpos receptores propiedad de la nación."
//        ),
//        25 to mapOf(
//            "clasificacion" to "AGUA RESIDUAL",
//            "clave_interna" to "CI-EST-025",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-002-ECOL-1996, Que establece los límites máximos permisibles de contaminantes en las descargas de aguas residuales a los sistemas de alcantarillado urbano o municipal."
//        ),
//        26 to mapOf(
//            "clasificacion" to "PTAR",
//            "clave_interna" to "CI-EST-026",
//            "cantidad_toma" to "NULL",
//            "norma" to "NOM-001-SEMARNAT-2021, Que establece los límites permisibles de contaminantes en las descargas de aguas residuales en cuerpos receptores propiedad de la nación."
//        ),
//        27 to mapOf(
//            "clasificacion" to "PTAR",
//            "clave_interna" to "CI-EST-027",
//            "cantidad_toma" to "NULL",
//            "norma" to "NOM-002-ECOL-1996, Que establece los límites máximos permisibles de contaminantes en las descargas de aguas residuales a los sistemas de alcantarillado urbano o municipal."
//        ),
//        28 to mapOf(
//            "clasificacion" to "LODO",
//            "clave_interna" to "CI-EST-028",
//            "cantidad_toma" to "NULL",
//            "norma" to "NOM-004-SEMARNAT-2002, Protección ambiental.- Lodos y biosólidos.-Especificaciones y límites máximos permisibles de contaminantes para su aprovechamiento y disposición final."
//        ),
//        29 to mapOf(
//            "clasificacion" to "LEGIONELLA",
//            "clave_interna" to "CI-EST-029",
//            "cantidad_toma" to "NULL",
//            "norma" to "UNE 100030:2017 Prevención y control de la proliferación y diseminación de Legionella en instalaciones."
//        ),
//        30 to mapOf(
//            "clasificacion" to "Ma",
//            "clave_interna" to "CI-EST-030",
//            "cantidad_toma" to "NULL",
//            "norma" to "UNE 100030:2017 Prevención y control de la proliferación y diseminación de Mesofilicos Aerobios en instalaciones."
//        ),
//        31 to mapOf(
//            "clasificacion" to "SALMONELLA",
//            "clave_interna" to "CI-EST-031",
//            "cantidad_toma" to "NULL",
//            "norma" to "Perfil de estudio para determinación de Salmonella."
//        ),
//        32 to mapOf(
//            "clasificacion" to "VIBRIO CHOLERAE",
//            "clave_interna" to "CI-EST-032",
//            "cantidad_toma" to "NULL",
//            "norma" to "Perfil de estudio para determinación de Vibrio Cholerae."
//        ),
//        33 to mapOf(
//            "clasificacion" to "HIERRO",
//            "clave_interna" to "CI-EST-033",
//            "cantidad_toma" to "NULL",
//            "norma" to "Perfil de estudios para determinación de HIERRO."
//        ),
//        34 to mapOf(
//            "clasificacion" to "E.COLI",
//            "clave_interna" to "CI-EST-034",
//            "cantidad_toma" to "NULL",
//            "norma" to "Perfil de estudios para determinación de E.COLI."
//        ),
//        35 to mapOf(
//            "clasificacion" to "STAPHYLOCOCCUS AUREUS",
//            "clave_interna" to "CI-EST-035",
//            "cantidad_toma" to "NULL",
//            "norma" to "Perfil de estudios para determinación de STAPHYLOCOCCUS."
//        ),
//        36 to mapOf(
//            "clasificacion" to "SERVICIO DE RECOLECCION DE MUESTRAS",
//            "clave_interna" to "CI-EST-036",
//            "cantidad_toma" to "NULL",
//            "norma" to "N/A."
//        ),
//        37 to mapOf(
//            "clasificacion" to "AGUA TRATADA",
//            "clave_interna" to "CI-EST-037",
//            "cantidad_toma" to "100ml",
//            "norma" to "NOM-003-ECOL-1997, Que establece los límites máximos permisibles de contaminantes para las aguas residuales tratadas que se reusen en servicios al público."
//        ),
//        38 to mapOf(
//            "clasificacion" to "PROTEINAS CRUDAS",
//            "clave_interna" to "CI-EST-038",
//            "cantidad_toma" to "300gr",
//            "norma" to "NOM-213-SSA1-2018, Productos y servicios. Productos cárnicos procesados y los establecimientos dedicados a su proceso. Disposiciones y especificaciones sanitarias. Métodos de prueba.; NOM-242-SSA1-2009, Productos de la pesca frescos, refrigerados, congelados y procesados. Especificaciones sanitarias y métodos de prueba."
//        )
//    )
    private lateinit var adapterEdicion: muestraAdapter
    private lateinit var adapter: muestraAdapter
    private var modoEdicion = false
    private var muestraMutableList: MutableList<Muestra> = mutableListOf()
    private var fechaSinBarras: String = ""
    private var lugares: ArrayList<String> = ArrayList()
    var contador = 0
    private val storagePermissionRequestCode = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMuestraExtraBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        binding.tvfechamuestreo.text = currentDate

        clientePdm =   intent.getParcelableExtra("clientePdm")
        pdmDetallado = intent.getParcelableExtra("pdmDetallado")!!
        lugares = intent.getStringArrayListExtra("lugares") ?: ArrayList()
        println("La lista de lugares es: $lugares")
        for(lugar in lugares){
            println("El lugar es: $lugar")
        }




        val adapterLugares =
            ArrayAdapter(this@MuestraExtraActivity, android.R.layout.simple_spinner_dropdown_item, lugares)

        binding.txtLugar.setAdapter(adapterLugares)

        try {


            val muestrasExistentes = intent.getParcelableArrayListExtra<Muestra>("muestraList")
            if (muestrasExistentes != null) {
                Log.e("muestrasExistentes", muestrasExistentes.toString())
                muestraMutableList.addAll(muestrasExistentes)
                contador = muestraMutableList.size
                binding.tvNumeroMuestra.text = (contador + 1).toString()
                adapter.notifyDataSetChanged()
            }else{
                Toast.makeText(this, "No hay muestras", Toast.LENGTH_SHORT).show()
                contador = 0
            }

        }catch (e: Exception){
            Log.e("Error", "Error al obtener la lista de muestras:" + e)
            Toast.makeText(this, "No hay muestras", Toast.LENGTH_SHORT).show()
        }
        folio = intent.getStringExtra("folio")
        folio = folio + "E"



        val tvRegM  =binding.tvregistromuestra
        val tvnum = binding.tvNumeroMuestra
        binding.tvFolio.text = folio
        binding.tvCliente.text = clientePdm?.nombre_empresa
        binding.tvPDM.text = pdmDetallado.nombre_pdm


        tvRegM.text = folio + "-" + tvnum.text.toString()

        var x = 0
        for (x in 1..38){
            arregloDeNumeros.add(x)
        }
        var sepudo = false
        binding.btnStart.setOnClickListener {

            if (modoEdicion == true){
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Confirmación")
                builder.setMessage("¿Estás seguro de que deseas editar la muestra?")
                builder.setPositiveButton("Sí") { dialog, which ->
//                    try {
//
//
//                        if (binding.txtnombre.text.trim().isEmpty() ||
//                            binding.txtcantidadaprox.text.trim().isEmpty() ||
//                            binding.txtTemp.text.trim().isEmpty() ||
//                            binding.txtLugar.text.trim().isEmpty() ||
//                            binding.txtdescripcion.text.trim().isEmpty()
//                        ) {
//
//                            Toast.makeText(
//                                this,
//                                "Por favor, complete todos los campos",
//                                Toast.LENGTH_SHORT
//                            ).show()
//
//                        } else {
//                            muestraMutableList[indexMuestraAEditar].nombreMuestra =
//                                binding.txtnombre.text.toString()
//                            muestraMutableList[indexMuestraAEditar].cantidadAprox =
//                                binding.txtcantidadaprox.text.toString()
//                            muestraMutableList[indexMuestraAEditar].tempM =
//                                binding.txtTemp.text.toString()
//                            muestraMutableList[indexMuestraAEditar].lugarToma =
//                                binding.txtLugar.text.toString()
//                            muestraMutableList[indexMuestraAEditar].descripcionM =
//                                binding.txtdescripcion.text.toString()
//                            muestraMutableList[indexMuestraAEditar].emicro =
//                                binding.txtMicro.text.toString()
//                            muestraMutableList[indexMuestraAEditar].efisico =
//                                binding.txtFisico.text.toString()
//                            muestraMutableList[indexMuestraAEditar].observaciones =
//                                binding.txtobservaciones.text.toString()
//
//                            adapterEdicion?.notifyItemChanged(indexMuestraAEditar)
//                            clearTextFields()
//                            Toast.makeText(this, "Muestra editada", Toast.LENGTH_SHORT).show()
//                            setEditMode(false)
//                            binding.tvTitulo.text = "Registro de Muestras"
//                            binding.btnStart.text = "Agregar"
//                            indexMuestraAEditar = -1
//                            binding.idSpinner1.isEnabled = true
//
//                        }
//
//                    } catch (e: Exception) {
//                        Log.e("Error".toString(), "Hubo un error ${e}")
//                        setEditMode(false)
//                        binding.tvTitulo.text = "Registro de Muestras"
//                        binding.btnStart.text = "Agregar"
//                        binding.idSpinner1.isEnabled = true
//                        clearTextFields()
//                        Toast.makeText(
//                            this,
//                            "Error al editar la muestra, Saliendo del modo edicion",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        indexMuestraAEditar = -1
//                    }


                }

                builder.setNegativeButton("No") { dialog, which ->
                    dialog.dismiss()
                }

                builder.setNeutralButton("Cancelar Edicion") { dialog, which ->
                    setEditMode(false)
                    binding.tvTitulo2.text = "Registro de Muestras Extra"
                    binding.btnStart.text = "Agregar"
                    clearTextFields()
                    Toast.makeText(
                        this,
                        "Saliendo del modo edicion",
                        Toast.LENGTH_SHORT
                    ).show()
                    //indexMuestraAEditar = -1
                }

                builder.show()





            }else{
                //Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                sepudo = createMuestra()
                if (sepudo == true) {
                    binding.tvregistromuestra.text = binding.tvFolio.text.toString() + "-" + binding.tvNumeroMuestra.text.toString()
                    clearTextFields()
                    Log.i("Ray", "Boton Pulsado")
                }
                checkStoragePermissionAndSaveJson()
            }



        }






        binding.btnAceptar.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmación")
            builder.setMessage("¿Estás seguro de que deseas guardar las muestras extras?")
            builder.setPositiveButton("Sí") { dialog, which ->
                checkStoragePermissionAndSaveJson()
                val resultIntent = Intent()
                resultIntent.putParcelableArrayListExtra("muestrasList", ArrayList(muestraMutableList))
                setResult(Activity.RESULT_OK, resultIntent)
                finish() // Cierra la actividad

            }

            builder.setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }

            builder.show()



        }

        binding.btnInfo.setOnClickListener{
            showServicioDialog()
        }


        var descripcionesLista = intent.getParcelableArrayListExtra<Descripcion>("descripciones")
        if (descripcionesLista != null) {
            descripcionesList.addAll(descripcionesLista)
            println("La lista de descripciones es: $descripcionesList")
        }

        val descris =
            descripcionesList.map { it.descripcion.toString() } // Convertir IDs a Strings
        // Configurar Autocompleteview
        val adapterDesci = ArrayAdapter(
            this@MuestraExtraActivity,
            android.R.layout.simple_spinner_dropdown_item,
            descris
        )
        Log.e("descris", descris.toString())
        binding.txtdescripcion.setAdapter(adapterDesci)
        binding.txtLugar.setOnClickListener(View.OnClickListener {
            binding.txtLugar.showDropDown()
        })

        binding.txtdescripcion.setOnClickListener(View.OnClickListener {
            binding.txtdescripcion.showDropDown()
        })
        val spinner = binding.idSpinner1
        val txtCantidadAprox = binding.txtcantidadaprox

        val adapter = ArrayAdapter(this@MuestraExtraActivity, android.R.layout.simple_spinner_item, arregloDeNumeros)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter


        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                val selectedItem = parent?.getItemAtPosition(position)
                binding.tvdescripcionmuestra.text = estudios[selectedItem]?.get("clasificacion").toString()+"-"+estudios[selectedItem]?.get("norma").toString()
                try {
                    txtCantidadAprox.text.clear()
                    try {
                        txtCantidadAprox.text = Editable.Factory.getInstance()
                            .newEditable(estudios[selectedItem]?.get("cantidad_toma"))
                    } catch (e: Exception) {
                        Log.e(
                            "Error",
                            "Error al establecer la cantidad aproximada en txtCantidadAprox"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("Error", "Error al limpiar txtCantidadAprox")
                }
            }


            override fun onNothingSelected(p0: AdapterView<*>?) {
                //nada
            }

        }

        initRecyclerView()
    }



    override fun onBackPressed() {
        // Crear un AlertDialog para la confirmación
        AlertDialog.Builder(this).apply {
            setTitle("Confirmación")
            setMessage("¿Estás seguro de que deseas salir?")
            setPositiveButton("Sí") { dialog, _ ->
                arregloDeNumeros.clear()
                dialog.dismiss()
                super.onBackPressed() // Llamar al método onBackPressed original
                finish()

            }
            setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Cerrar el cuadro de diálogo y no hacer nada más
            }
            create()
            show()
        }
    }

    private fun showServicioDialog() {


        val dialogView = layoutInflater.inflate(R.layout.dialog_servicio_list, null)
        val recyclerView: RecyclerView = dialogView.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = EstudiosAdapterInfo(estudiosList)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Lista de Estudios")
            .setView(dialogView)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()
    }


    private fun createMuestra(): Boolean {
        var sepudo = false
        val tvNum = binding.tvNumeroMuestra
        val tvfecham = binding.tvfechamuestreo
        //val tvhoram = binding.tvHora
        val tvregistromuestra = binding.tvregistromuestra
        val txtnombrem = binding.txtnombre
        val txtcantidad = binding.txtcantidadaprox
        val txtTemp = binding.txtTemp
        val txtLugar = binding.txtLugar
        val txtDescripcion = binding.txtdescripcion
        val txtMicro = binding.txtMicro
        val txtFisico = binding.txtFisico
        val txtObserva = binding.txtobservaciones
        val txtServicioId = binding.idSpinner1
        val idServicioString = txtServicioId.selectedItem.toString()
        var idServicioEntero: String = String()
        val spinner1 = binding.idSpinner1

        if (txtnombrem.text.toString().trim().isEmpty() || txtcantidad.text.toString().trim()
                .isEmpty() || txtTemp.text.toString().trim().isEmpty() || txtLugar.text.toString()
                .trim().isEmpty() || txtDescripcion.text.toString().trim().isEmpty()
        )
//        var f=false
//        if (f==true)
        {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            sepudo = false
        } else {

            val numeroMuestra = tvNum.text

            if (numeroMuestra != null) {
                val formatoEntrada = SimpleDateFormat("MM/dd/yyyy")
                val formatoSalida = SimpleDateFormat("yyyyMMdd")

                val fecha = formatoEntrada.parse(tvfecham.text.toString())

                // Formatea la fecha al nuevo formato sin barras
                fechaSinBarras = formatoSalida.format(fecha)


                //val fechaSinBarras = tvfecham.text.toString().replace("/", "")
                //val horaSinPuntos = tvhoram.text.toString().replace(":", "")
                /*val horaRecortada =
                    if (horaSinPuntos.length >= 4) horaSinPuntos.substring(
                        0,
                        4
                    ) else horaSinPuntos*/
                val idLab = fechaSinBarras + tvregistromuestra.text.toString()

                // El valor de idServicio es un entero válido, puedes usarlo aquí
                val muestraobjeto =
                    Muestra(
                        numeroMuestra = numeroMuestra.toString(),
                        fechaMuestra = tvfecham.text.toString(),
                        registroMuestra = tvregistromuestra.text.toString(),
                        nombreMuestra = txtnombrem.text.toString().trim(),
                        idLab = idLab,
                        cantidadAprox = txtcantidad.text.toString().trim(),
                        tempM = txtTemp.text.toString().trim()+"°C",
                        lugarToma = txtLugar.text.toString().trim(),
                        descripcionM = txtDescripcion.text.toString().trim(),
                        emicro = txtMicro.text.toString().trim(),
                        efisico = txtFisico.text.toString().trim(),
                        observaciones = txtObserva.text.toString().trim(),
                        servicioId = "",
                        idEstudio = spinner1.selectedItem.toString()
                    )
                muestraMutableList.add(muestraobjeto)
                contador = muestraMutableList.size
                tvNum.text = (contador + 1).toString()
                Log.i("creadeis", muestraobjeto.toString())


                adapter.notifyItemInserted(muestraMutableList.size - 1)
                Toast.makeText(this, "Se ha añadido la muestra", Toast.LENGTH_SHORT).show()
                sepudo = true
            } else {
                // Manejar el caso donde la conversión falló
                Toast.makeText(this, "Por favor, ingrese un número válido", Toast.LENGTH_SHORT)
                    .show()
                Log.i("Ray", "Ingrese numero valido")

            }


        }

        //Toast.makeText(this, "El estado de sepudo es: $sepudo", Toast.LENGTH_SHORT).show()
        return sepudo
    }

    private fun initRecyclerView() {
        //val recyclerView = findViewById<RecyclerView>(R.id.recyclerMuestras)
        //adapter = muestraAdapter(){}
        adapter = muestraAdapter(
            muestraList = muestraMutableList,
            onClickListener = { muestra -> onItemSelected(muestra) },
            onclickDelete = { position -> onDeletedItem(position) },
            onclickEdit = { position -> onEditItem(position) },
            this)

        binding.recyclerMuestras.layoutManager = LinearLayoutManager(this)
        binding.recyclerMuestras.adapter = adapter
        val callback = DragManageAdapter(adapter) {modoEdicion}
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.recyclerMuestras)


    }
    override fun onItemMoved() {
        // Implementa la lógica para guardar los datos cuando un item se mueva
        checkStoragePermissionAndSaveJson()
        Log.i("Ray", "Se ha movido un item")
    }


    private fun onItemSelected(muestra: Muestra) { //pendiente
        if (modoEdicion == true){
            Toast.makeText(this, "No se puede copiar una muestra en modo edicion", Toast.LENGTH_SHORT).show()
        }else{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmación")
            builder.setMessage("¿Estás seguro de que deseas copiar la muestra ${muestra.nombreMuestra}?")
            builder.setPositiveButton("Sí") { dialog, which ->
                try {
                    clearTextFields()
                    binding.txtnombre.setText(muestra.nombreMuestra)
                    binding.txtTemp.setText(muestra.tempM)
                    binding.txtLugar.setText(muestra.lugarToma)
                    binding.txtdescripcion.setText(muestra.descripcionM)
                } catch (e: Exception) {
                    Log.e("Error".toString(), "Hubo un error ${e}")
                }

            }

            builder.setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }

            builder.show()



            Toast.makeText(this, muestra.nombreMuestra, Toast.LENGTH_SHORT).show()
            Log.i("Ray", muestra.nombreMuestra)
        }

    }

    private fun onDeletedItem(position: Int) {
        if (modoEdicion == true || muestraMutableList[position].observaciones.contains( "Eliminada" )){
            Toast.makeText(this, "No se puede eliminar una muestra en modo  o que haya sido eliminada", Toast.LENGTH_SHORT).show()
        }else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmación")
            val input = EditText(this)
            input.hint = "Ingrese el motivo de la eliminación"
            builder.setView(input)

            builder.setMessage("¿Estás seguro de que deseas eliminar la muestra?")

            builder.setPositiveButton("Sí") { dialog, which ->
                try {

                    val motivoEliminacion = input.text.toString().trim()
                    if (motivoEliminacion.isEmpty()) {
                        Toast.makeText(this, "Debe ingresar un motivo para la eliminación", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val muestraEliminada = muestraMutableList[position]
                    muestraEliminada.observaciones = "Eliminada - Motivo: $motivoEliminacion"


                    val spinner1 = binding.idSpinner1


//                    muestraMutableList.removeAt(position)
//                    //Notificar al listado que se ha en este caso borrado un item con una posicion
//                    adapter.notifyItemRemoved(position)
                    val tvFolio = binding.tvFolio


                    // Actualizar los números de muestra en la lista
//                    for (i in position until muestraMutableList.size) {
//                        muestraMutableList[i].numeroMuestra = (i + 1).toString()
//                        muestraMutableList[i].registroMuestra =
//                            tvFolio.text.toString() + "-" + muestraMutableList[i].numeroMuestra
//                        muestraMutableList[i].idLab = fechaSinBarras+tvFolio.text.toString() + "-" + muestraMutableList[i].numeroMuestra
//
//                    }
//                    adapter.notifyItemRangeChanged(position, muestraMutableList.size)

                    // Actualizar contador y TextView de número de muestra
//                    contador = muestraMutableList.size
//                    binding.tvNumeroMuestra.text = (contador + 1).toString()
//                    binding.tvregistromuestra.text =
//                        tvFolio.text.toString() + "-" + binding.tvNumeroMuestra.text.toString()
                    Log.e("Prueba".toString(), "El contador es:$contador")

                    checkStoragePermissionAndSaveJson()


                } catch (e: Exception) {
                    Log.e("Error".toString(), "Hubo un error")
                }

            }

            // Configurar el botón "No"
            builder.setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }

            // Mostrar el cuadro de diálogo
            builder.show()

        }


    }

    private fun checkStoragePermissionAndSaveJson() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + applicationContext.packageName)
                startActivityForResult(intent, storagePermissionRequestCode)
            } else {
                val serviciospdm: List<Servicio> = emptyList()
                val muestraData = MuestraData(
                    binding.tvFolio.text.toString(),
                    pdmDetallado.nombre_pdm,
                    clientePdm,
                    serviciospdm,
                    muestraMutableList,
                    pdmDetallado,
                    ArrayList()
                )
                saveDataToJson(this, muestraData, "Datos-folioExtra-${binding.tvFolio.text}.json")
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
                val muestraData = MuestraData(
                    binding.tvFolio.text.toString(),
                    pdmDetallado.nombre_pdm,
                    clientePdm,
                    emptyList(),
                    muestraMutableList,
                    pdmDetallado,
                    ArrayList()
                )
                saveDataToJson(this, muestraData, "Datos-folioExtra-${binding.tvFolio.text}.json")
            }
        }
    }

    private fun clearTextFields() {

        val txtnombrem = binding.txtnombre
        val txtTemp = binding.txtTemp
        val txtLugar = binding.txtLugar
        val txtDescripcion = binding.txtdescripcion
        val txtObserva = binding.txtobservaciones

        txtnombrem.text.clear()
        txtTemp.text.clear()
        txtLugar.text.clear()
        txtDescripcion.text.clear()
        txtObserva.text.clear()

    }
    fun saveDataToJson(context: Context, muestraData: MuestraData, filename: String) {
        val gson = Gson()
        val jsonString = gson.toJson(muestraData)

        // Obtener la ruta de la carpeta Documents
        val documentsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .toString()


        // Crear el archivo en la carpeta Documents
        val file = File(documentsDir, filename)

        // Escribir el archivo
        file.writeText(jsonString)
    }
    fun setEditMode(editMode: Boolean) {
        modoEdicion = editMode
        // Aquí puedes hacer otras acciones necesarias cuando el modo edición cambia
    }
    private fun onEditItem(position: Int) {
        Toast.makeText(this, "En desarrollo",Toast.LENGTH_SHORT)
    }
}