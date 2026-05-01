package com.example.dragonbudget.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dragonbudget.ui.screens.*

object Routes {
    const val HOME = "home"
    const val ADD_PURCHASE = "add_purchase"
    const val BUDGETS = "budgets"
    const val ASK_DRAGON = "ask_dragon"
    const val HISTORY = "history"
}

@Composable
fun DragonBudgetNavHost(appContainer: com.example.dragonbudget.AppContainer) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(150))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(150))
        }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                appContainer = appContainer,
                onNavigateToAddPurchase = { navController.navigate(Routes.ADD_PURCHASE) },
                onNavigateToAskDragon = { navController.navigate(Routes.ASK_DRAGON) },
                onNavigateToBudgets = { navController.navigate(Routes.BUDGETS) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) }
            )
        }
        composable(Routes.ADD_PURCHASE) {
            AddPurchaseScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.BUDGETS) {
            BudgetScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ASK_DRAGON) {
            AskDragonScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
