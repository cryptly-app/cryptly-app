package com.cryptlysafe.cryptly.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cryptlysafe.cryptly.auth.LoginScreen
import com.cryptlysafe.cryptly.app.GeneralModeScreen
import com.cryptlysafe.cryptly.private.PrivateHomeScreen
import com.cryptlysafe.cryptly.chat.ChatScreen
import com.cryptlysafe.cryptly.calls.PrivateCallLogScreen

object AppRoutes {
    const val LOGIN = "login"
    const val GENERAL = "general"
    const val PRIVATE_PIN = "private_pin"
    const val PRIVATE_HOME = "private_home"
    const val CHAT = "chat"
    const val CALL_LOG = "call_log"
}

sealed class Screen(val route: String) {
    object Login : Screen(AppRoutes.LOGIN)
    object General : Screen(AppRoutes.GENERAL)
    object PrivatePin : Screen(AppRoutes.PRIVATE_PIN)
    object PrivateHome : Screen(AppRoutes.PRIVATE_HOME)
    object Chat : Screen(AppRoutes.CHAT)
    object CallLog : Screen(AppRoutes.CALL_LOG)
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen()
        }
        composable(Screen.General.route) {
            GeneralModeScreen()
        }
        composable(Screen.PrivateHome.route) {
            PrivateHomeScreen()
        }
        composable(Screen.Chat.route) {
            ChatScreen(chatId = "sample123")
        }
        composable(Screen.CallLog.route) {
            PrivateCallLogScreen()
        }
    }
}
