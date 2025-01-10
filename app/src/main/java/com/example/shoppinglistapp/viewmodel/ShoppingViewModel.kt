package com.example.shoppinglistapp.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoppinglistapp.data.ShoppingItem
import com.example.shoppinglistapp.network.NetworkUtils
import com.example.shoppinglistapp.repository.ShoppingRepository
import kotlinx.coroutines.launch

class ShoppingViewModel(private val repository: ShoppingRepository) : ViewModel() {

    val shoppingItems = mutableStateOf<List<ShoppingItem>>(emptyList())

    fun fetchItems(context: Context) {
        viewModelScope.launch {
            try {
                if (NetworkUtils.isOnline(context)) {
                    val apiItems = repository.fetchShoppingItemsFromApi()
                    repository.deleteAllItems()
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
                } else {
                    println("Offline mode: Loading items from the database.")
                }
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
