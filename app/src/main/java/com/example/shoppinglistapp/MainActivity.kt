package com.example.shoppinglistapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shoppinglistapp.data.ShoppingDatabase
import com.example.shoppinglistapp.data.ShoppingItem
import com.example.shoppinglistapp.ui.theme.ShoppingListAppTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.shoppinglistapp.repository.ShoppingRepository
import com.example.shoppinglistapp.viewmodel.ShoppingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val db = ShoppingDatabase.getDatabase(this)

        lifecycleScope.launch {
            val dao = db.shoppingDao()

            // Pre-populate some items
            dao.insertItem(ShoppingItem(name = "Milk", quantity = 2, price = 1.5))
            dao.insertItem(ShoppingItem(name = "Bread", quantity = 1, price = 2.0))
            dao.insertItem(ShoppingItem(name = "Butter", quantity = 3, price = 3.5))
        }

        super.onCreate(savedInstanceState)
        setContent {
            ShoppingListAppTheme {
                ShoppingListApp()
            }
        }
    }
}

@Composable
fun ShoppingListApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            ShoppingListScreen(navController)
        }
        composable("details/{itemId}") { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            ShoppingDetailsScreen(itemId = itemId)
        }
    }
}

@Composable
fun EditItemDialog(
    item: ShoppingItem,
    onDismiss: () -> Unit,
    onSave: (ShoppingItem) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var quantity by remember { mutableStateOf(item.quantity.toString()) }
    var price by remember { mutableStateOf((item.price / item.quantity).toString()) } // Calculate unit price

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = quantity,
                    onValueChange = { input ->
                        val newQuantity = input.toIntOrNull() ?: 0
                        if (newQuantity > 0) {
                            // Update quantity and calculate price
                            quantity = newQuantity.toString()
                            price = ((item.price / item.quantity) * newQuantity).toString()
                        }
                    },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedItem = item.copy(
                    name = name.trim(),
                    quantity = quantity.toIntOrNull() ?: item.quantity,
                    price = price.toDoubleOrNull() ?: item.price
                )
                onSave(updatedItem)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}



@Composable
fun ShoppingListScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = ShoppingDatabase.getDatabase(context)
    val dao = db.shoppingDao()
    val scope = rememberCoroutineScope()

    val viewModel = remember {
        ShoppingViewModel(
            ShoppingRepository(
                ShoppingDatabase.getDatabase(context).shoppingDao()
            )
        )
    }

    // Fetching items from the API when the screen loads
    LaunchedEffect(Unit) {
        viewModel.fetchItems()
    }

    val scaffoldState = rememberScaffoldState()

    // State to hold shopping items
    var shoppingItems by remember { mutableStateOf(emptyList<ShoppingItem>()) }
    var itemName by remember { mutableStateOf("") }
    var itemQuantity by remember { mutableStateOf(1) }
    var itemPrice by remember { mutableStateOf(0.0) }

    // Dialog state for editing
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ShoppingItem?>(null) }

    // Load all items initially
    LaunchedEffect(Unit) {
        shoppingItems = dao.getAllItems()
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { TopAppBar(title = { Text("Shopping List") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    try {
                        if (itemName.isBlank() || itemQuantity <= 0 || itemPrice <= 0.0) {
                            scaffoldState.snackbarHostState.showSnackbar(
                                "Invalid inputs. Ensure all fields are filled correctly."
                            )
                        } else {
                            dao.insertItem(
                                ShoppingItem(name = itemName, quantity = itemQuantity, price = itemPrice)
                            )
                            itemName = ""
                            itemQuantity = 1
                            itemPrice = 0.0
                            shoppingItems = dao.getAllItems()
                        }
                    } catch (e: Exception) {
                        scaffoldState.snackbarHostState.showSnackbar(
                            "Error inserting item: ${e.message}"
                        )
                    }
                }
            }) {
                Text("Add")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            // Input Fields
            TextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = itemQuantity.toString(),
                onValueChange = { input -> itemQuantity = input.toIntOrNull() ?: 0 },
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = itemPrice.toString(),
                onValueChange = { input -> itemPrice = input.toDoubleOrNull() ?: 0.0 },
                label = { Text("Price") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // List of Shopping Items
            LazyColumn {
                items(shoppingItems) { item ->
                    ShoppingItemRow(
                        item = item,
                        navController = navController,
                        onDelete = {
                            scope.launch {
                                dao.deleteItemById(it.id)
                                shoppingItems = dao.getAllItems()
                            }
                        },
                        onEdit = {
                            selectedItem = it
                            showEditDialog = true
                        }
                    )
                }
            }
        }

        if (showEditDialog && selectedItem != null) {
            EditItemDialog(
                item = selectedItem!!,
                onDismiss = { showEditDialog = false },
                onSave = { updatedItem ->
                    scope.launch {
                        dao.updateItem(updatedItem)
                        shoppingItems = dao.getAllItems()
                    }
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    navController: NavHostController,
    onDelete: (ShoppingItem) -> Unit,
    onEdit: (ShoppingItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { navController.navigate("details/${item.id}") },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = item.name, style = MaterialTheme.typography.subtitle1)
            Text(text = "Qty: ${item.quantity} Price: $${item.price}", style = MaterialTheme.typography.body2)
        }
        Row {
            Button(onClick = { onEdit(item) }) {
                Text("Edit")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onDelete(item) }, colors = ButtonDefaults.buttonColors(MaterialTheme.colors.error)) {
                Text("Delete")
            }
        }
    }
}

@Composable
fun ShoppingDetailsScreen(itemId: String?) {
    val context = LocalContext.current
    val db = ShoppingDatabase.getDatabase(context)
    val dao = db.shoppingDao()
    var shoppingItem by remember { mutableStateOf<ShoppingItem?>(null) }

    // Load item details based on the ID
    LaunchedEffect(itemId) {
        if (itemId != null) {
            shoppingItem = dao.getItemById(itemId.toInt())
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Item Details") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            shoppingItem?.let {
                Text("Name: ${it.name}", style = MaterialTheme.typography.h5)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Quantity: ${it.quantity}", style = MaterialTheme.typography.body1)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Price: $${it.price}", style = MaterialTheme.typography.body1)
            } ?: Text("Loading item details...")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ShoppingListAppTheme {
        ShoppingListApp()
    }
}
