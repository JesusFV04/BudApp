package com.example.budapp.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarTransaccionScreen(navController: NavController, type: String) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    var categoryList by remember { mutableStateOf<List<String>>(emptyList()) }

    // Cargar categorías globales y personalizadas
    LaunchedEffect(user?.uid, type) {
        if (user == null) return@LaunchedEffect

        val global = db.collection("categories")
            .whereEqualTo("type", type)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("name") }

        val custom = db.collection("users")
            .document(user.uid)
            .collection("custom_categories")
            .whereEqualTo("type", type)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("name") }

        categoryList = (global + custom).sorted() + "Agregar nueva categoría..."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (type == "income") "Agregar ingreso" else "Agregar gasto",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Monto") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción (opcional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dropdown de categorías
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedCategory ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoría") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .clickable { expanded = true }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categoryList.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            expanded = false
                            if (category == "Agregar nueva categoría...") {
                                showCategoryDialog = true
                            } else {
                                selectedCategory = category
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val parsedAmount = amount.toDoubleOrNull()
                if (parsedAmount == null || parsedAmount <= 0.0) {
                    Toast.makeText(context, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (selectedCategory.isNullOrBlank()) {
                    Toast.makeText(context, "Selecciona una categoría", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val uid = user?.uid ?: return@Button

                val data = mapOf(
                    "type" to type,
                    "amount" to parsedAmount,
                    "description" to description,
                    "category" to selectedCategory,
                    "date" to System.currentTimeMillis()
                )

                db.collection("users")
                    .document(uid)
                    .collection("transactions")
                    .add(data)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Transacción guardada", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar")
        }
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val name = newCategoryName.trim()
                    if (name.isNotEmpty() && user != null) {
                        val data = mapOf("name" to name, "type" to type)
                        db.collection("users")
                            .document(user.uid)
                            .collection("custom_categories")
                            .add(data)
                            .addOnSuccessListener {
                                selectedCategory = name
                                categoryList = categoryList.toMutableList().apply {
                                    add(size - 1, name)
                                }
                                showCategoryDialog = false
                                newCategoryName = ""
                            }
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCategoryDialog = false
                    newCategoryName = ""
                }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Nueva categoría") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Nombre de la categoría") }
                )
            }
        )
    }
}
