package com.example.shoppinglistapp.network

data class ApiShoppingItem(
    val id: Int,
    val title: String,
    val price: Double,
    val description: String,
    val category: String,
    val image: String
)
