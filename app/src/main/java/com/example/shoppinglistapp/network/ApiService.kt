package com.example.shoppinglistapp.network
import retrofit2.http.GET

interface ApiService {
    @GET("products")
    suspend fun getShoppingItems(): List<ApiShoppingItem>
}