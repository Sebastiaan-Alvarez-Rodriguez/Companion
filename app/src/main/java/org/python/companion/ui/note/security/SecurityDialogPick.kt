package org.python.companion.ui.note.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Password
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.python.backend.security.SecurityActor
import org.python.companion.R

@Composable
fun SecurityPickDialog(
    headerText: String,
    onDismiss: () -> Unit,
    onItemClick: (Int) -> Unit,
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(defaultPadding)) {
                Text(
                    text = headerText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(defaultPadding)
                )
                Spacer(modifier = Modifier.height(defaultPadding))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        modifier = Modifier.padding(tinyPadding),
                        onClick = { onItemClick(SecurityActor.TYPE_PASS) }) {
                        Icon(Icons.Filled.Password, "Password login")
                    }
                    Spacer(modifier = Modifier.width(defaultPadding))
                    IconButton(
                        modifier = Modifier.padding(tinyPadding),
                        onClick = { onItemClick(SecurityActor.TYPE_BIO) }) {
                        Icon(Icons.Filled.Fingerprint, "Biometric login")
                    }
                }
            }
        }
    }
}
