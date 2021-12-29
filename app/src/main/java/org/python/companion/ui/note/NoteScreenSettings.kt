package org.python.companion.ui.note

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import org.python.companion.R


/**
 * NoteScreen settings screen, where users can change several settings.
 * @param onExportClick Lambda to perform on note export clicks.
 * @param onImportClick Lambda to perform on note import clicks.
 */
@Composable
fun NoteScreenSettings(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    Card(elevation = 5.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(modifier = Modifier.fillMaxWidth().padding(defaultPadding), onClick = onExportClick) {
                Text(text = "Export")
            }
            Button(modifier = Modifier.fillMaxWidth().padding(defaultPadding), onClick = onImportClick) {
                Text(text = "Import")
            }
        }
    }
}

@Composable
fun NoteScreenSettingsTransfers(exporting: Boolean) {

}