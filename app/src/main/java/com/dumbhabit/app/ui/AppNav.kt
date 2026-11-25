package com.dumbhabit.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dumbhabit.app.ui.screens.HabitsScreen
import com.dumbhabit.app.ui.screens.HomeScreen
import com.dumbhabit.app.ui.screens.PomodoroScreen
import com.dumbhabit.app.ui.screens.RoutineEditScreen
import com.dumbhabit.app.ui.screens.RoutineRunScreen
import com.dumbhabit.app.ui.screens.RoutinesScreen
import com.dumbhabit.app.ui.screens.SettingsScreen

@Composable
fun AppNav(vm: AppViewModel) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                vm = vm,
                onHabits = { nav.navigate("habits") },
                onRoutines = { nav.navigate("routines") },
                onSettings = { nav.navigate("settings") },
                onPomodoro = { nav.navigate("pomodoro") },
                onRunStarted = { nav.navigate("run") },
            )
        }
        composable("pomodoro") {
            PomodoroScreen(onBack = { nav.popBackStack() })
        }
        composable("habits") {
            HabitsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable("routines") {
            RoutinesScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate("routine/$id") },
                onRunStarted = { nav.navigate("run") },
            )
        }
        composable(
            "routine/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            RoutineEditScreen(
                vm = vm,
                routineId = entry.arguments?.getLong("id") ?: 0L,
                onBack = { nav.popBackStack() },
            )
        }
        composable("run") {
            RoutineRunScreen(vm = vm, onFinish = { nav.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}
