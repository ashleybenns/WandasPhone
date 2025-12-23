package com.wandasphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wandasphone.core.ui.theme.ThemeOption
import com.wandasphone.core.ui.theme.WandasPhoneTheme
import com.wandasphone.feature.home.HomeScreen
import com.wandasphone.feature.phone.InCallScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WandasPhoneApp()
        }
    }
}

@Composable
fun WandasPhoneApp() {
    val navController = rememberNavController()
    
    // TODO: Load theme from settings, for now use default
    WandasPhoneTheme(themeOption = ThemeOption.HIGH_CONTRAST_LIGHT) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToCall = {
                        navController.navigate("call")
                    },
                    onNavigateToCarer = {
                        navController.navigate("carer")
                    }
                )
            }
            
            composable("call") {
                InCallScreen(
                    onNavigateBack = {
                        navController.popBackStack("home", inclusive = false)
                    }
                )
            }
            
            composable("carer") {
                com.wandasphone.feature.carer.CarerScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

