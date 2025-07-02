package com.example.budapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var transacciones by remember { mutableStateOf<List<Transaccion>>(emptyList()) }

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
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (transacciones.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay transacciones registradas.")
            }
        } else {
            LazyColumn {
                items(transacciones) { txn ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${if (txn.type == "income") "Ingreso" else "Gasto"}: $${"%.2f".format(txn.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (txn.type == "income") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Text("Categoría: ${txn.category}")
                            if (txn.description.isNotBlank()) {
                                Text("Descripción: ${txn.description}")
                            }
                            val formattedDate = remember(txn.date) {
                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(txn.date))
                            }
                            Text("Fecha: $formattedDate", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
