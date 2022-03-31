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
fun SecurityBioDialogContent(
    onNegativeClick: () -> Unit,
    onPositiveClick: () -> Unit,
    state: LoadingState = LoadingState.READY,
    stateMessage: String? = null
) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            when(state) {
                LoadingState.READY, LoadingState.FAILED ->
                    SecurityBioDialogReady(onNegativeClick, onPositiveClick, stateMessage)
                LoadingState.LOADING -> SimpleLoading()
                LoadingState.OK -> SimpleOk()
            }
        }
}

@Composable
private fun SecurityBioDialogReady(
    onNegativeClick: () -> Unit,
    onPositiveClick: () -> Unit,
    stateMessage: String? = null
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    dimensionResource(id = R.dimen.padding_small)
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)

    Column(modifier = Modifier.padding(defaultPadding)) {
        Text(
            text = "Please authenticate",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(defaultPadding)
        )
        Spacer(modifier = Modifier.height(defaultPadding))

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                modifier = Modifier.padding(tinyPadding),
                onClick = { onPositiveClick() }) {
                Icon(Icons.Filled.Fingerprint, "Start biometric login")
            }
        }

        Spacer(modifier = Modifier.height(defaultPadding))

        if (stateMessage != null)
            Text(text = stateMessage, fontSize = 8.sp)

        Spacer(modifier = Modifier.height(defaultPadding))
        TextButton(onClick = onNegativeClick) {
            Text(text = "CANCEL")
        }
    }
}