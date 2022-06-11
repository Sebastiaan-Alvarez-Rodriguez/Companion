package org.python.companion.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.python.companion.R
import org.python.companion.support.LoadingState
import org.python.companion.support.UiUtil.SimpleLoading
import org.python.companion.support.UiUtil.SimpleOk

@Composable
fun SecurityBioDialogSetup(
    onNegativeClick: () -> Unit,
    onPositiveClick: () -> Unit
) {
    SecurityBioDialogGeneric(onNegativeClick, onPositiveClick, "Setup")
}

@Composable
fun SecurityBioDialogReset(
    onNegativeClick: () -> Unit,
    onPositiveClick: () -> Unit
) {
    SecurityBioDialogGeneric(onNegativeClick, onPositiveClick, "Reset")
}

@Composable
private fun SecurityBioDialogGeneric(
    onNegativeClick: () -> Unit,
    onPositiveClick: () -> Unit,
    type: String
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Card(elevation = 8.dp, modifier = Modifier.padding(defaultPadding).wrapContentHeight(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(defaultPadding)) {
            Text(
                text = "Fingerprint $type",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(defaultPadding)
            )
            Spacer(modifier = Modifier.height(defaultPadding))

            Text(
                text = "Fingerprint ${type.lowercase()} is handled by the Android Operating System.",
                modifier = Modifier.padding(defaultPadding)
            )
            Spacer(modifier = Modifier.height(defaultPadding))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onNegativeClick) {
                    Text(text = "CANCEL")
                }
                TextButton(onClick = onPositiveClick) {
                    Text("FINGERPRINT SETTINGS")
                }
            }
        }
    }
}