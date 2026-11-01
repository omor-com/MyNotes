package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.example.data.backup.BackupManager
import com.example.data.local.database.AppDatabase
import com.example.data.preferences.ThemePreference
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.NoteRepository
import com.example.ui.components.MyNotesBottomBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.FolderDetailScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.MyNotesTheme
import com.example.ui.viewmodel.FoldersViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.NoteEditorViewModel
import com.example.ui.viewmodel.SearchViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.TrashViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Local Storage & Repository
        val database = AppDatabase.getInstance(applicationContext)
        val preferencesRepo = UserPreferencesRepository(applicationContext)
        val backupManager = BackupManager(
            context = applicationContext,
            noteDao = database.noteDao(),
            folderDao = database.folderDao(),
            tagDao = database.tagDao(),
            checklistDao = database.checklistDao()
        )
        val googleDriveBackupManager = com.example.data.backup.GoogleDriveBackupManager(applicationContext)
        val noteRepository = NoteRepository(
            noteDao = database.noteDao(),
            folderDao = database.folderDao(),
            tagDao = database.tagDao(),
            checklistDao = database.checklistDao(),
            preferencesRepository = preferencesRepo,
            backupManager = backupManager,
            googleDriveBackupManager = googleDriveBackupManager
        )

        // Perform daily automatic Google Drive backup in background if due
        CoroutineScope(Dispatchers.IO).launch {
            if (noteRepository.checkDailyDriveBackupNeeded()) {
                noteRepository.backupToGoogleDrive()
            }
        }

        setContent {
            val themePref by preferencesRepo.themePreference.collectAsState(initial = ThemePreference.SYSTEM)
            val isDarkTheme = when (themePref) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }

            MyNotesTheme(darkTheme = isDarkTheme) {
                MyNotesApp(repository = noteRepository)
            }
        }
    }
}

@Composable
fun MyNotesApp(repository: NoteRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ViewModel Factories
    val homeViewModel: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(repository) as T
            }
        }
    )

    val foldersViewModel: FoldersViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FoldersViewModel(repository) as T
            }
        }
    )

    val searchViewModel: SearchViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(repository) as T
            }
        }
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(repository) as T
            }
        }
    )

    val trashViewModel: TrashViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TrashViewModel(repository) as T
            }
        }
    )

    // Show bottom bar only on primary top-level tabs
    val showBottomBar = currentRoute == Screen.Home.route ||
            currentRoute == Screen.Folders.route ||
            currentRoute == Screen.Settings.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                MyNotesBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home Screen
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToEditor = { noteId, folderId, type ->
                        navController.navigate(Screen.NoteEditor.createRoute(noteId, folderId, type))
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            // Note Editor Screen
            composable(
                route = Screen.NoteEditor.route,
                arguments = listOf(
                    navArgument("noteId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("folderId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("type") { type = NavType.StringType; defaultValue = "RICH_TEXT" }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                val rawFolderId = backStackEntry.arguments?.getLong("folderId") ?: -1L
                val folderId = if (rawFolderId >= 0) rawFolderId else null
                val type = backStackEntry.arguments?.getString("type") ?: "RICH_TEXT"

                val editorViewModel: NoteEditorViewModel = viewModel(
                    key = "editor_$noteId",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return NoteEditorViewModel(repository, noteId, folderId, type) as T
                        }
                    }
                )

                NoteEditorScreen(
                    viewModel = editorViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Folders Screen
            composable(Screen.Folders.route) {
                FoldersScreen(
                    viewModel = foldersViewModel,
                    onNavigateToFolderDetail = { folderId, folderName ->
                        navController.navigate(Screen.FolderDetail.createRoute(folderId, folderName))
                    }
                )
            }

            // Folder Detail Screen
            composable(
                route = Screen.FolderDetail.route,
                arguments = listOf(
                    navArgument("folderId") { type = NavType.LongType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "Folder" }
                )
            ) { backStackEntry ->
                val folderId = backStackEntry.arguments?.getLong("folderId") ?: 0L
                val folderName = backStackEntry.arguments?.getString("name") ?: "Folder"

                FolderDetailScreen(
                    folderId = folderId,
                    folderName = folderName,
                    foldersViewModel = foldersViewModel,
                    homeViewModel = homeViewModel,
                    onNavigateToEditor = { noteId, fId, type ->
                        navController.navigate(Screen.NoteEditor.createRoute(noteId, fId, type))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Search Screen
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = searchViewModel,
                    onNavigateToEditor = { noteId ->
                        navController.navigate(Screen.NoteEditor.createRoute(noteId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToTrash = {
                        navController.navigate(Screen.Trash.route)
                    }
                )
            }

            // Trash Screen
            composable(Screen.Trash.route) {
                TrashScreen(
                    viewModel = trashViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
