package com.example.shoppinglistapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoppinglistapp.data.ShoppingItem
import com.example.shoppinglistapp.network.NetworkUtils
import com.example.shoppinglistapp.repository.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ShoppingViewModel(private val repository: ShoppingRepository) : ViewModel() {

    // Backing property for shoppingItems
    private val _shoppingItems = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val shoppingItems: StateFlow<List<ShoppingItem>> = _shoppingItems

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
                // Update the StateFlow
                _shoppingItems.value = repository.getAllShoppingItems()
            } catch (e: Exception) {
                println("Error fetching items: ${e.message}")
            }
        }
    }

    fun updateItem(updatedItem: ShoppingItem) {
        viewModelScope.launch {
            repository.updateItem(updatedItem)
            _shoppingItems.value = repository.getAllShoppingItems() // Refresh the list
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.deleteItemById(item.id)
            _shoppingItems.value = repository.getAllShoppingItems() // Refresh the list
        }
    }
}
