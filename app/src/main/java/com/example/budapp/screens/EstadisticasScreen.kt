package com.example.budapp.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class CategoriaEstadistica(val nombre: String, val total: Double)
data class EvolucionSaldo(val fecha: String, val saldo: Double)

@Composable
fun EstadisticasScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()

    var ingresos by remember { mutableStateOf(0.0) }
    var gastos by remember { mutableStateOf(0.0) }
    var categoriasGasto by remember { mutableStateOf<List<CategoriaEstadistica>>(emptyList()) }
    var evolucion by remember { mutableStateOf<List<EvolucionSaldo>>(emptyList()) }
    var dias by remember { mutableStateOf(15) }

    LaunchedEffect(uid, dias) {
        uid?.let {
            db.collection("users")
                .document(it)
                .collection("transactions")
                .get()
                .addOnSuccessListener { snapshot ->
                    var totalIngresos = 0.0
                    var totalGastos = 0.0
                    val mapaCategorias = mutableMapOf<String, Double>()
                    val mapaSaldoPorDia = sortedMapOf<String, Double>()

                    val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val inicio = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -dias + 1) }

                    for (i in 0 until dias) {
                        val cal = Calendar.getInstance().apply { time = inicio.time }
                        cal.add(Calendar.DAY_OF_YEAR, i)
                        val clave = formato.format(cal.time)
                        mapaSaldoPorDia[clave] = 0.0
                    }

                    var saldoAcumulado = 0.0

                    snapshot.documents.sortedBy { it.getLong("date") ?: 0L }
                        .forEach { doc ->
                            val type = doc.getString("type") ?: ""
                            val amount = (doc.getDouble("amount") ?: 0.0)
                            val category = doc.getString("category") ?: "Otros"
                            val timestamp = doc.getLong("date") ?: 0L
                            val fechaStr = formato.format(Date(timestamp))

                            if (fechaStr in mapaSaldoPorDia.keys) {
                                saldoAcumulado += if (type == "income") amount else -amount
                                mapaSaldoPorDia[fechaStr] = saldoAcumulado
                            }

                            if (type == "income") {
                                totalIngresos += amount
                            } else {
                                totalGastos += amount
                                mapaCategorias[category] =
                                    mapaCategorias.getOrDefault(category, 0.0) + amount
                            }
                        }

                    ingresos = totalIngresos
                    gastos = totalGastos
                    categoriasGasto = mapaCategorias.map { CategoriaEstadistica(it.key, it.value) }
                    evolucion = mapaSaldoPorDia.map { EvolucionSaldo(it.key, it.value) }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Estadísticas", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoBox("Ingresos", ingresos, colorScheme.primary)
            InfoBox("Gastos", gastos, colorScheme.error)
            InfoBox(
                "Balance",
                ingresos - gastos,
                if (ingresos - gastos >= 0) colorScheme.primary else colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (categoriasGasto.isNotEmpty()) {
            Text("Distribución de gastos por categoría", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            DonutChart(data = categoriasGasto)
            Spacer(modifier = Modifier.height(16.dp))
            categoriasGasto.forEach {
                Text("${it.nombre}: ${NumberFormat.getCurrencyInstance().format(it.total)}")
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Aún no hay gastos registrados por categoría.")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Evolución del saldo (${dias} días)", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { dias = 15 }) { Text("Últimos 15 días") }
            Button(onClick = { dias = 30 }) { Text("Últimos 30 días") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (evolucion.isNotEmpty()) {
            SaldoLineChart(evolucion)
        } else {
            Text("Aún no hay datos para mostrar el saldo.")
        }

        Spacer(modifier = Modifier.height(32.dp)) // ⬅️ Evita que se corte por la navegación
    }
}

@Composable
fun InfoBox(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            text = NumberFormat.getCurrencyInstance().format(amount),
            color = color,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
fun DonutChart(data: List<CategoriaEstadistica>) {
    val total = data.sumOf { it.total }
    val colors = listOf(
        Color(0xFF4CAF50), Color(0xFFF44336), Color(0xFF2196F3),
        Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFF009688)
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        var startAngle = -90f
        data.forEachIndexed { i, cat ->
            val porcentaje = (cat.total / total).toFloat()
            val sweepAngle = (porcentaje * 360f).coerceAtLeast(3f) // 👈 Corregido aquí
            drawArc(
                color = colors[i % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 32f)
            )
            startAngle += sweepAngle
        }
    }
}


@Composable
fun SaldoLineChart(data: List<EvolucionSaldo>) {
    val puntos = data.mapIndexed { index, it -> index to it.saldo }

    val maxY = puntos.maxOfOrNull { it.second } ?: 0.0
    val minY = puntos.minOfOrNull { it.second } ?: 0.0
    val range = (maxY - minY).takeIf { it != 0.0 } ?: 1.0

    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(220.dp)
        .padding(horizontal = 16.dp, vertical = 8.dp)) {

        val widthStep = size.width / (puntos.size - 1).coerceAtLeast(1)

        puntos.forEachIndexed { i, (x, y) ->
            val px = i * widthStep
            val py = size.height - ((y - minY) / range * size.height).toFloat()

            if (i < puntos.size - 1) {
                val nextY = puntos[i + 1].second
                val nextPx = (i + 1) * widthStep
                val nextPy = size.height - ((nextY - minY) / range * size.height).toFloat()

                drawLine(
                    color = lineColor,
                    start = Offset(px, py),
                    end = Offset(nextPx, nextPy),
                    strokeWidth = 4f
                )
            }

            drawCircle(
                color = lineColor,
                radius = 6f,
                center = Offset(px, py)
            )
        }
    }
}
