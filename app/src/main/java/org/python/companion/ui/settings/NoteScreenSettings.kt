package org.python.companion.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import org.python.companion.R


/**
 * NoteScreen settings screen, where users can change several settings.
 * @param onSecuritySetupClick Lambda to perform on security reset clicks.
 * @param onSecurityResetClick Lambda to perform on security reset clicks.
 * @param onExportClick Lambda to perform on note export clicks.
 * @param onImportClick Lambda to perform on note import clicks.
 * @param onBackClick Lambda to perform on back clicks.
 */
@Composable
fun NoteScreenSettings(
    onSecuritySetupClick: () -> Unit,
    onSecurityResetClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Column(modifier = Modifier.fillMaxSize().padding(defaultPadding)) {
        ViewHeader(onBackClick = onBackClick)

        Spacer(Modifier.height(defaultPadding))

        Card(elevation = 5.dp) {
            Column {
                Text("Security", modifier = Modifier.padding(defaultPadding))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(defaultPadding)) {
                    Button(onClick = onSecuritySetupClick) {
                        Text(text = "Setup")
                    }
                    Button(onClick = onSecurityResetClick) {
                        Text(text = "Reset")
                    }
                }
            }
        }

        Spacer(Modifier.height(defaultPadding))

        Card(elevation = 5.dp) {
            Column {
                Text("Backups", modifier = Modifier.padding(defaultPadding))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(defaultPadding)) {
                    Button(onClick = onImportClick) {
                        Text(text = "Import")
                    }
                    Button(onClick = onExportClick) {
                        Text(text = "Export")
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewHeader(onBackClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBackClick) {
            Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Back")
        }
    }
}
