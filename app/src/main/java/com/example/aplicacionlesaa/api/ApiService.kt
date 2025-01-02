package com.example.aplicacionlesaa.api

import com.example.aplicacionlesaa.model.ClientePdm
import com.example.aplicacionlesaa.model.DatosFinalesFolioMuestreo
import com.example.aplicacionlesaa.model.Descripcion
import com.example.aplicacionlesaa.model.FolioMuestreo
import com.example.aplicacionlesaa.model.Lugar
import com.example.aplicacionlesaa.model.Lugares
import com.example.aplicacionlesaa.model.Muestra_pdm
import com.example.aplicacionlesaa.model.Muestra_pdmExtra
import com.example.aplicacionlesaa.model.Pdm
import com.example.aplicacionlesaa.model.Plandemuestreo
import com.example.aplicacionlesaa.model.Servicio
import com.example.aplicacionlesaa.model.UltimoFolio
import com.example.aplicacionlesaa.model.analisisFisico
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("planes")
    fun getPlanes(): Call<List<Plandemuestreo>>

    @GET("planesrecortado")
    fun getPlanesRecortado(): Call<List<Pdm>>

//    @GET("posts")
//    fun getPosts(): Call<List<Post>>

    @GET("plan/{id}")
    fun getPlanServicesByName(@Path("id") id: String): Call<List<Servicio>>

    @GET("descripciones")
    fun getDescriptions(): Call<List<Descripcion>>

    @GET("plancliente/{id}")
    fun getPlanClienteByPdmName(@Path("id") id: String): Call<ClientePdm>

    @GET("planinfo/{id}")
    fun getPlanInfoByPdmName(@Path("id") id: String): Call<Pdm>

    @GET("clientelugar/{id}")
    fun getClienteLugarById(@Path("id") id: String): Call<List<Lugares>>



    @POST("crearfoliomuestreo")
    fun createFolioMuestreo(@Body folioMuestreo: FolioMuestreo): Call<Void>


    @GET("ultimofoliomuestreo")
    fun getLastFolioMuestreo(): Call<UltimoFolio>


    @POST("crearmuestra")
    fun createMuestreo(@Body muestra: Muestra_pdm): Call<Void>

    @POST("createmuestraextra")
    fun createMuestreoExtra(@Body muestra: Muestra_pdmExtra): Call<Void>

    @POST("createfoliomuestreoextra")
    fun createFolioMuestreoExtra(@Body folioMuestreo: FolioMuestreo): Call<Void>

    @POST("createclientelugar")
    fun createLugarCliente(@Body lugar: Lugar): Call<Void>

    @PUT("restarservicio/{id}")
    fun restarServicio(@Path("id") id: String, @Body data: RestarServicioRequest): Call<Void>

    data class RestarServicioRequest(
        val cantidad: Int
    )

    @PUT("completarfolio/{id}")
    fun completarFolio(@Path("id") id: String, @Body data: DatosFinalesFolioMuestreo): Call<Void>



    @POST("createfisicoquimicos")
    fun createFisicoquimicos(@Body fisico: analisisFisico): Call<Void>




//    @GET("posts/{id}")
//    fun getPostById(@Path("id") id: Int): Call<Post>
}
