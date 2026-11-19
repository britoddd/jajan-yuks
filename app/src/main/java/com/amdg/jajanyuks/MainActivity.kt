package com.amdg.jajanyuks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amdg.jajanyuks.domain.Role
import com.amdg.jajanyuks.ui.auth.LoginScreen
import com.amdg.jajanyuks.ui.auth.RegisterScreen
import com.amdg.jajanyuks.ui.auth.StartScreen
import com.amdg.jajanyuks.ui.buyer.BuyerScreen
import com.amdg.jajanyuks.ui.buyer.VendorDetailScreen
import com.amdg.jajanyuks.ui.seller.SellerScreen
import com.amdg.jajanyuks.ui.theme.JajanYuksTheme
import kotlinx.serialization.Serializable

// Type-safe navigation: each screen is a @Serializable object instead of a string route.
@Serializable object StartRoute
@Serializable object LoginRoute
@Serializable object RegisterRoute
@Serializable object SellerRoute
@Serializable object BuyerRoute
@Serializable data class VendorDetailRoute(val vendorId: String) // a route that carries an argument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val users = (application as JajanYuksApp).container.userRepository
        // setContent replaces XML layouts: the UI is built from @Composable functions.
        setContent {
            JajanYuksTheme {
                val nav = rememberNavController()
                val goHome = { role: Role -> nav.replaceAll(if (role == Role.SELLER) SellerRoute else BuyerRoute) }
                val logout = {
                    users.signOut()
                    nav.replaceAll(LoginRoute)
                }
                Scaffold { padding ->
                    NavHost(nav, startDestination = StartRoute, modifier = Modifier.padding(padding)) {
                        composable<StartRoute> {
                            StartScreen(users) { role -> if (role == null) nav.replaceAll(LoginRoute) else goHome(role) }
                        }
                        composable<LoginRoute> {
                            LoginScreen(onLoggedIn = goHome, onGoToRegister = { nav.navigate(RegisterRoute) })
                        }
                        composable<RegisterRoute> {
                            RegisterScreen(onRegistered = goHome, onGoToLogin = { nav.popBackStack() })
                        }
                        composable<SellerRoute> { SellerScreen(onLogout = logout) }
                        composable<BuyerRoute> {
                            BuyerScreen(onOpenVendor = { nav.navigate(VendorDetailRoute(it)) }, onLogout = logout)
                        }
                        composable<VendorDetailRoute> { VendorDetailScreen(onBack = { nav.popBackStack() }) }
                    }
                }
            }
        }
    }
}

// Navigate and clear the back stack, so "back" can't return to login or a logged-out screen.
private fun NavHostController.replaceAll(route: Any) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
}
