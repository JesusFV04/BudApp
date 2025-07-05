// Mismo package y imports que ya tienes
package com.example.budapp.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class Transaccion(
    val id: String = "",
    val type: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val date: Long = 0L
)

@Composable
fun HistorialScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var transacciones by remember { mutableStateOf<List<Transaccion>>(emptyList()) }
    var filtroFecha by remember { mutableStateOf("Últimos 7 días") }
    var filtroTipo by remember { mutableStateOf("Todos") }
    var fechaSeleccionada by remember { mutableStateOf<Long?>(null) }

    val opcionesFecha = listOf("Últimos 7 días", "Últimos 30 días", "Seleccionar fecha específica")
    val opcionesTipo = listOf("Todos", "Ingresos", "Gastos")

    // Combinación de filtros
    val transaccionesFiltradas = remember(transacciones, filtroFecha, fechaSeleccionada, filtroTipo) {
        val now = System.currentTimeMillis()
        var lista = when (filtroFecha) {
            "Últimos 7 días" -> transacciones.filter { it.date >= now - 7 * 24 * 60 * 60 * 1000 }
            "Últimos 30 días" -> transacciones.filter { it.date >= now - 30 * 24 * 60 * 60 * 1000 }
            "Seleccionar fecha específica" -> {
                fechaSeleccionada?.let { fecha ->
                    val cal = Calendar.getInstance().apply { timeInMillis = fecha }
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    val end = start + 24 * 60 * 60 * 1000
                    transacciones.filter { it.date in start until end }
                } ?: transacciones
            }
            else -> transacciones
        }

        when (filtroTipo) {
            "Ingresos" -> lista.filter { it.type == "income" }
            "Gastos" -> lista.filter { it.type == "expense" }
            else -> lista
        }
    }

    LaunchedEffect(uid) {
        uid?.let {
            db.collection("users")
                .document(it)
                .collection("transactions")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    val lista = snapshot?.documents?.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        Transaccion(
                            id = doc.id,
                            type = data["type"] as? String ?: "",
                            amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                            description = data["description"] as? String ?: "",
                            category = data["category"] as? String ?: "",
                            date = (data["date"] as? Number)?.toLong() ?: 0L
                        )
                    } ?: emptyList()
                    transacciones = lista
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Historial de transacciones",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filtros
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Filtro fecha
            var expandedFecha by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expandedFecha = true }) {
                    Text(filtroFecha)
                }
                DropdownMenu(
                    expanded = expandedFecha,
                    onDismissRequest = { expandedFecha = false }
                ) {
                    opcionesFecha.forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion) },
                            onClick = {
                                filtroFecha = opcion
                                expandedFecha = false
                                if (opcion == "Seleccionar fecha específica") {
                                    val cal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            cal.set(year, month, day)
                                            fechaSeleccionada = cal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                } else {
                                    fechaSeleccionada = null
                                }
                            }
                        )
                    }
                }
            }

            // Filtro tipo
            var expandedTipo by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expandedTipo = true }) {
                    Text(filtroTipo)
                }
                DropdownMenu(
                    expanded = expandedTipo,
                    onDismissRequest = { expandedTipo = false }
                ) {
                    opcionesTipo.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo) },
                            onClick = {
                                filtroTipo = tipo
                                expandedTipo = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (transaccionesFiltradas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay transacciones para este filtro.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(transaccionesFiltradas) { txn ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (txn.type == "income") Icons.Default.AttachMoney else Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = if (txn.type == "income") Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(32.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${txn.category} - $${"%.2f".format(txn.amount)}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = if (txn.type == "income") Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                                if (txn.description.isNotBlank()) {
                                    Text(
                                        text = txn.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                val formattedDate = remember(txn.date) {
                                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(txn.date))
                                }
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
