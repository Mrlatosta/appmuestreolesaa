package com.example.aplicacionlesaa.api

import com.example.aplicacionlesaa.model.Plandemuestreo
import com.example.aplicacionlesaa.model.Servicio
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("planes")
    fun getPlanes(): Call<List<Plandemuestreo>>

//    @GET("posts")
//    fun getPosts(): Call<List<Post>>

    @GET("plan/{id}")
    fun getPlanServicesByName(@Path("id") id: String): Call<List<Servicio>>

//    @GET("posts/{id}")
//    fun getPostById(@Path("id") id: Int): Call<Post>
}
