package org.python.companion.ui.settings.exim

import androidx.navigation.NavHostController
import org.python.companion.support.UiUtil

internal object Shared {
    const val PARQUET_EXTENSION = "pq"
    const val NOTEFILE_NAME = "notes.pq"
    const val NOTECATEGORYFILE_NAME = "notecategories.$PARQUET_EXTENSION"

    fun navigateToStop(navController: NavHostController, isExport: Boolean = true, onStopClick: () -> Unit) =
        UiUtil.UIUtilState.navigateToBinary(
            navController = navController,
            title = "${if (isExport) "Exporting" else "Importing"} unfinished",
            message = "Are you sure you want to go back? ${if (isExport) "Export" else "Import"} process will be cancelled.",
            positiveText = "Stop ${if (isExport) "exporting" else "importing"}",
            onOptionClick = { if (it) onStopClick() }
        )
}