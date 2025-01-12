package com.example.shoppinglistapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale



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
    val systemDarkTheme = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemDarkTheme) }
    val navController = rememberNavController()

    ShoppingListAppTheme(darkTheme = isDarkTheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Shopping List") },
                    actions = {
                        IconButton(onClick = { isDarkTheme = !isDarkTheme }) {
                            Icon(
                                imageVector = Icons.Default.Brightness6,
                                contentDescription = "Toggle Theme"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "list",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("list") {
                    ShoppingListScreen(navController)
                }
                composable("details/{itemId}") { backStackEntry ->
                    val itemId = backStackEntry.arguments?.getString("itemId")
                    ShoppingDetailsScreen(itemId = itemId)
                }
            }
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
    var price by remember { mutableStateOf((item.price / item.quantity).toString()) } // Unit price

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
                            quantity = newQuantity.toString()
                            price = ((item.price / item.quantity) * newQuantity).toString()
                        }
                    },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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
    val viewModel = remember {
        ShoppingViewModel(
            ShoppingRepository(
                ShoppingDatabase.getDatabase(context).shoppingDao()
            )
        )
    }

    // Observe shopping items from the ViewModel
    val shoppingItems = viewModel.shoppingItems.collectAsState(emptyList())
    //var isRefreshing by remember { mutableStateOf(false) }

    var selectedItem by remember { mutableStateOf<ShoppingItem?>(null) } // Use var for reassignment
    var showEditDialog by remember { mutableStateOf(false) }

    // Fetch items on first load
    LaunchedEffect(Unit) {
        viewModel.fetchItems(context)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Add new item logic */ }) {
                Text("+")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            if (shoppingItems.value.isEmpty()) {
                Text("No items available. Check your connection or add items manually.")
            } else {
                LazyColumn {
                    items(shoppingItems.value) { shoppingItem ->
                        ShoppingItemCard(
                            item = shoppingItem,
                            navController = navController,
                            onDelete = { selectedItem ->
                                viewModel.deleteItem(selectedItem)
                            },
                            onEdit = { shoppingItem ->
                                selectedItem = shoppingItem // Update selectedItem here
                                showEditDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Edit Dialog
        if (showEditDialog && selectedItem != null) {
            EditItemDialog(
                item = selectedItem!!,
                onDismiss = { showEditDialog = false },
                onSave = { updatedItem ->
                    viewModel.updateItem(updatedItem)
                    showEditDialog = false
                }
            )
        }
    }
}




@Composable
fun ShoppingItemCard(
    item: ShoppingItem,
    navController: NavHostController,
    onDelete: (ShoppingItem) -> Unit,
    onEdit: (ShoppingItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { expanded = !expanded }
            .animateContentSize(),
        elevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.name, style = MaterialTheme.typography.subtitle1)
            if (expanded) {
                Text(text = "Qty: ${item.quantity} Price: $${item.price}", style = MaterialTheme.typography.body2)
                Row {
                    IconButton(onClick = { onEdit(item) }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onDelete(item) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
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
                // Display a larger image (placeholder used here)
                Image(
                    painter = painterResource(R.drawable.placeholder), // Replace with your image logic
                    contentDescription = "Item Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.Crop
                )
                Text("Name: ${it.name}", style = MaterialTheme.typography.h5)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Quantity: ${it.quantity}", style = MaterialTheme.typography.body1)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Price: $${it.price}", style = MaterialTheme.typography.body1)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Description: Sample item description here.", style = MaterialTheme.typography.body2)
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
