package com.amdg.jajanyuks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amdg.jajanyuks.ui.auth.RoleScreen
import com.amdg.jajanyuks.ui.buyer.BuyerScreen
import com.amdg.jajanyuks.ui.seller.SellerScreen
import com.amdg.jajanyuks.ui.theme.JajanYuksTheme
import kotlinx.serialization.Serializable

// Type-safe navigation: each screen is a @Serializable object instead of a string route.
@Serializable object RoleRoute
@Serializable object SellerRoute
@Serializable object BuyerRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as JajanYuksApp).container
        // setContent replaces XML layouts: the UI is built from @Composable functions.
        setContent {
            JajanYuksTheme {
                val navController = rememberNavController()
                Scaffold { padding ->
                    NavHost(navController, startDestination = RoleRoute, modifier = Modifier.padding(padding)) {
                        composable<RoleRoute> {
                            RoleScreen(
                                auth = container.auth,
                                onSeller = { navController.navigate(SellerRoute) },
                                onBuyer = { navController.navigate(BuyerRoute) },
                            )
                        }
                        composable<SellerRoute> { SellerScreen() }
                        composable<BuyerRoute> { BuyerScreen() }
                    }
                }
            }
        }
    }
}
