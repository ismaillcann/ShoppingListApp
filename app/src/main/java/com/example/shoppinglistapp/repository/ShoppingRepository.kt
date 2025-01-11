package com.example.shoppinglistapp.repository

import com.example.shoppinglistapp.data.ShoppingDao
import com.example.shoppinglistapp.data.ShoppingItem
import com.example.shoppinglistapp.network.ApiShoppingItem
import com.example.shoppinglistapp.network.RetrofitInstance

class ShoppingRepository(private val dao: ShoppingDao) {

    suspend fun fetchShoppingItemsFromApi(): List<ApiShoppingItem> {
        return try {
            RetrofitInstance.api.getShoppingItems()
        } catch (e: Exception) {
            println("API Error: ${e.message}")
            emptyList()
        }
    }

    suspend fun updateItem(item: ShoppingItem) {
        dao.updateItem(item)
    }


    suspend fun insertShoppingItem(item: ShoppingItem) {
        dao.insertItem(item)
    }

    suspend fun getAllShoppingItems(): List<ShoppingItem> {
        return dao.getAllItems()
    }

    suspend fun deleteItemById(id: Int) {
        dao.deleteItemById(id)
    }

    suspend fun deleteAllItems() {
        dao.deleteAllItems()
    }
}
