package com.example.shoppinglistapp.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoppinglistapp.data.ShoppingItem
import com.example.shoppinglistapp.repository.ShoppingRepository
import kotlinx.coroutines.launch

class ShoppingViewModel(private val repository: ShoppingRepository) : ViewModel() {

    val shoppingItems = mutableStateOf<List<ShoppingItem>>(emptyList())

    fun fetchItems() {
        viewModelScope.launch {
            try {
                // Fetch from API
                val apiItems = repository.fetchShoppingItemsFromApi()
                println("API Items Fetched: $apiItems")

                repository.deleteAllItems() // Clear old data
                apiItems.forEach { apiItem ->
                    repository.insertShoppingItem(
                        ShoppingItem(
                            id = apiItem.id,
                            name = apiItem.title,
                            quantity = 1, // Default quantity
                            price = apiItem.price
                        )
                    )
                }

                // Fetch updated list from database
                shoppingItems.value = repository.getAllShoppingItems()
            } catch (e: Exception) {
                println("Error fetching items: ${e.message}")
            }
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.deleteItemById(item.id)
            shoppingItems.value = repository.getAllShoppingItems()
        }
    }
}
