package com.example.budapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.budapp.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var balance by remember { mutableStateOf(0.0) }

    // Escuchar cambios en tiempo real en las transacciones del usuario
    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            db.collection("users")
                .document(uid)
                .collection("transactions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Toast.makeText(context, "Error al obtener datos", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    var total = 0.0
                    snapshot?.documents?.forEach { doc ->
                        val amount = doc.getDouble("amount") ?: 0.0
                        val type = doc.getString("type")
                        total += if (type == "income") amount else -amount
                    }
                    balance = total
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hola, ${user?.email ?: "usuario"}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Balance actual",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$${"%.2f".format(balance)}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Acciones",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionButton(
            text = "Agregar ingreso",
            icon = Icons.Default.Add
        ) {
            navController.navigate(Screen.AddTransaction.createRoute("income"))
        }

        Spacer(modifier = Modifier.height(8.dp))

        ActionButton(
            text = "Agregar gasto",
            icon = Icons.Default.Warning
        ) {
            navController.navigate(Screen.AddTransaction.createRoute("expense"))
        }

        Spacer(modifier = Modifier.height(32.dp))

        ActionButton(
            text = "Ver historial",
            icon = Icons.Default.Info
        ) {
            navController.navigate(Screen.History.route)
        }
        Spacer(modifier = Modifier.height(8.dp))

        ActionButton(
            text = "Ver estadísticas",
            icon = Icons.Default.Info
        ) {
            navController.navigate(Screen.Stats.route)
        }

        Spacer(modifier = Modifier.height(32.dp))

        ActionButton(
            text = "Cerrar sesión",
            icon = Icons.Default.ExitToApp
        ) {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        }


    }
}

@Composable
fun ActionButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, contentDescription = text)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}
