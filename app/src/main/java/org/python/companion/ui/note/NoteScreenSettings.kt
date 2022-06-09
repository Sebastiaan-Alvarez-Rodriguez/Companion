package org.python.companion.ui.note

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.backend.data.datatype.NoteWithCategory
import org.python.backend.data.datatype.RenderType
import org.python.companion.R
import org.python.companion.support.RenderUtil


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
    }

    Spacer(Modifier.height(defaultPadding))

    Card(elevation = 5.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(defaultPadding)) {
            Button(modifier = Modifier.weight(0.5f, fill = false), onClick = onSecuritySetupClick) {
                Text(text = "Setup new security")
            }
            Button(modifier = Modifier.weight(0.5f, fill = false), onClick = onSecurityResetClick) {
                Text(text = "Reset security")
            }
        }
    }

    Spacer(Modifier.height(defaultPadding))

    Card(elevation = 5.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(defaultPadding)) {
            Button(modifier = Modifier.weight(0.5f, fill = false), onClick = onImportClick) {
                Text(text = "Import")
            }
            Button(modifier = Modifier.weight(0.5f, fill = false), onClick = onExportClick) {
                Text(text = "Export")
            }
        }
    }
}

@Composable
fun NoteScreenSettingsTransfers(exporting: Boolean) {

}

@Composable
private fun ViewHeader(onBackClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBackClick) {
            Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Back")
        }
    }
}
