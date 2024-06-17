package com.example.aplicacionlesaa.api

import com.example.aplicacionlesaa.model.ClientePdm
import com.example.aplicacionlesaa.model.Descripcion
import com.example.aplicacionlesaa.model.FolioMuestreo
import com.example.aplicacionlesaa.model.Muestra_pdm
import com.example.aplicacionlesaa.model.Pdm
import com.example.aplicacionlesaa.model.Plandemuestreo
import com.example.aplicacionlesaa.model.Servicio
import com.example.aplicacionlesaa.model.UltimoFolio
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("planes")
    fun getPlanes(): Call<List<Plandemuestreo>>

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

    @POST("crearfoliomuestreo")
    fun createFolioMuestreo(@Body folioMuestreo: FolioMuestreo): Call<Void>


    @GET("ultimofoliomuestreo")
    fun getLastFolioMuestreo(): Call<UltimoFolio>


    @POST("crearmuestra")
    fun createMuestreo(@Body muestra: Muestra_pdm): Call<Void>


//    @GET("posts/{id}")
//    fun getPostById(@Path("id") id: Int): Call<Post>
}
