package com.animate.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.animate.companion.ui.chat.ChatScreen
import com.animate.companion.ui.create.CreatorScreen
import com.animate.companion.ui.home.HomeScreen
import com.animate.companion.ui.settings.SettingsScreen
import com.animate.companion.ui.theme.AniMateTheme
import com.animate.companion.update.UpdateDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as AniMateApp).container
        setContent {
            AniMateTheme {
                AppNav(container)
                UpdateDialog(container.updates)
            }
        }
    }
}

@Composable
private fun AppNav(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { it / 4 } + fadeOut() },
    ) {
        composable("home") {
            HomeScreen(
                container = container,
                onCreate = { nav.navigate("create") },
                onOpen = { nav.navigate("chat/$it") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("create") {
            CreatorScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onCreated = { id ->
                    nav.navigate("chat/$id?greet=true") { popUpTo("home") }
                },
            )
        }
        composable(
            "chat/{id}?greet={greet}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("greet") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            ChatScreen(
                container = container,
                characterId = entry.arguments!!.getLong("id"),
                greet = entry.arguments!!.getBoolean("greet"),
                onBack = { nav.popBackStack() },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsScreen(container = container, onBack = { nav.popBackStack() })
        }
    }
}
