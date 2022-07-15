package org.python.companion.support

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

object PermissionUtil {
    @Composable
    inline fun requestExternalStoragePermission(
        navController: NavController,
        crossinline onGranted: () -> Unit
    ): ManagedActivityResultLauncher<String, Boolean> {
        return rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onGranted()
            } else {
                // TODO: Only ask for permission as per https://developer.android.com/training/permissions/requesting
                UiUtil.UIUtilState.navigateToSingular(
                    navController = navController,
                    title = "Permission needed, really",
                    message = "We need file access permission to import data from a file. We solely access files for importing/exporting.",
                    onClick = {}
                )
            }
        }
    }
}