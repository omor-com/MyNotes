package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Folders : Screen("folders")
    data object Search : Screen("search")
    data object Settings : Screen("settings")
    data object Trash : Screen("trash")

    data object NoteEditor : Screen("editor/{noteId}?folderId={folderId}&type={type}") {
        fun createRoute(noteId: Long, folderId: Long? = null, contentType: String? = null): String {
            val f = folderId ?: -1L
            val t = contentType ?: "RICH_TEXT"
            return "editor/$noteId?folderId=$f&type=$t"
        }
    }

    data object FolderDetail : Screen("folder/{folderId}?name={name}") {
        fun createRoute(folderId: Long, folderName: String): String {
            return "folder/$folderId?name=${android.net.Uri.encode(folderName)}"
        }
    }
}
