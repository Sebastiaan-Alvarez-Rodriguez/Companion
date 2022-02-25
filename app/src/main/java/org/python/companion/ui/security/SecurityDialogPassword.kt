package org.python.companion.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.python.backend.security.PasswordVerificationToken
import org.python.companion.R
import org.python.companion.support.LoadState
import org.python.companion.support.LoadingState
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.SimpleLoading
import org.python.companion.support.UiUtil.SimpleOk
import timber.log.Timber
import java.nio.ByteBuffer


@Composable
fun SecurityPasswordDialogContent(
    onNegativeClick: () -> Unit,
    onPositiveClick: (PasswordVerificationToken) -> Unit,
    state: UiUtil.StateMiniState
) {
    Timber.e("Got securityPasswordDialog state: $state (ready=${state.state.value == LoadState.STATE_READY}, loading=${state.state.value == LoadState.STATE_LOADING}, ok=${state.state.value == LoadState.STATE_OK}, failed=${state.state.value == LoadState.STATE_FAILED})")
    Card(elevation = 8.dp, shape = RoundedCornerShape(12.dp)) {
        when(state.state.value) {
            LoadState.STATE_READY, LoadState.STATE_FAILED ->
                SecurityPasswordDialogReady(onNegativeClick, onPositiveClick, state.state.value, state.stateMessage.value)
            LoadState.STATE_LOADING -> SimpleLoading()
            LoadState.STATE_OK -> SimpleOk()
        }
    }
}

@Composable
private fun SecurityPasswordDialogReady(
    onNegativeClick: () -> Unit,
    onPositiveClick: (PasswordVerificationToken) -> Unit,
    state: @LoadingState Int = LoadState.STATE_READY,
    stateMessage: String? = null
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    var pass by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(defaultPadding)) {
        Text(
            text = "Please authenticate",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(defaultPadding)
        )
        Spacer(modifier = Modifier.height(defaultPadding))

        OutlinedTextField(
            isError = state == LoadState.STATE_FAILED,
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Enter password...") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            trailingIcon = {
                val image = if (passVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passVisible = !passVisible }) {
                    Icon(imageVector = image, "")
                }
            }
        )
        if (stateMessage != null)
            Text(text = stateMessage, fontSize = 8.sp)

        Spacer(modifier = Modifier.height(defaultPadding))

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onNegativeClick) {
                Text(text = "CANCEL")
            }
            Spacer(modifier = Modifier.width(smallPadding))
            TextButton(onClick = {
                onPositiveClick(
                    PasswordVerificationToken.PassBuilder().with(
                        ByteBuffer.wrap(pass.toByteArray(charset = Charsets.ISO_8859_1))
                    ).build()
                )
            }) {
                Text(text = "SUBMIT")
            }
        }
    }
}
