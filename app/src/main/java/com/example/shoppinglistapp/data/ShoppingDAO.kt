package com.example.shoppinglistapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ShoppingDao {

    @Query("SELECT * FROM shopping_items")
    suspend fun getAllItems(): List<ShoppingItem>

    @Query("SELECT * FROM shopping_items WHERE id = :itemId")
    suspend fun getItemById(itemId: Int): ShoppingItem?

    @Insert
    suspend fun insertItem(item: ShoppingItem)

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM shopping_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Int)
}

