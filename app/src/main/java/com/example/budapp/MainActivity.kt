package com.example.budapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.budapp.navigation.Screen
import com.example.budapp.screens.*
import com.example.budapp.ui.theme.BudAppTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val user = FirebaseAuth.getInstance().currentUser
        val startDestination = if (user != null) Screen.Home.route else Screen.Login.route

        setContent {
            BudAppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(navController)
                        }
                        composable(Screen.Register.route) {
                            RegisterScreen(navController)
                        }
                        composable(Screen.Home.route) {
                            HomeScreen(navController)
                        }
                        composable(
                            route = Screen.AddTransaction.route,
                            arguments = listOf(navArgument("type") {
                                type = NavType.StringType
                                defaultValue = "income"
                            })
                        ) { backStackEntry ->
                            val type = backStackEntry.arguments?.getString("type") ?: "income"
                            AgregarTransaccionScreen(navController, type)
                        }
                        composable(Screen.History.route) {
                            HistorialScreen(navController)
                        }
                        composable(Screen.Stats.route) {
                            EstadisticasScreen(navController)
                        }

                    }
                }
            }
        }
    }
}
