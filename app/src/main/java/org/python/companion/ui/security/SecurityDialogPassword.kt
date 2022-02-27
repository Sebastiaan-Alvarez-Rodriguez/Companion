package org.python.companion.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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


@Composable
fun SecurityPasswordDialogContent(
    onNegativeClick: () -> Unit,
    onPositiveClick: (PasswordVerificationToken) -> Unit,
    onResetPasswordClick: () -> Unit,
    state: UiUtil.StateMiniState
) {
    Card(elevation = 8.dp, shape = RoundedCornerShape(12.dp)) {
        SecurityPasswordDialogReady(onNegativeClick, onPositiveClick, onResetPasswordClick, state.state.value, state.stateMessage.value)
    }
}

@Composable
private fun SecurityPasswordDialogReady(
    onNegativeClick: () -> Unit,
    onPositiveClick: (PasswordVerificationToken) -> Unit,
    onResetPasswordClick: () -> Unit,
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
            enabled = state != LoadState.STATE_LOADING,
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
            Text(text = stateMessage, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(defaultPadding))

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            when (state) {
                LoadState.STATE_LOADING -> CircularProgressIndicator()
                LoadState.STATE_OK -> Icon(imageVector = Icons.Filled.CheckCircle, "Ok")
                else -> TextButton(onClick = onResetPasswordClick) {
                    Text(text = "RESET PASSWORD")
                }
            }
            Row(horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onNegativeClick, enabled = state != LoadState.STATE_LOADING) {
                    Text(text = "CANCEL")
                }
                Spacer(modifier = Modifier.width(smallPadding))
                TextButton(
                    onClick = { onPositiveClick(PasswordVerificationToken.PassBuilder().with(pass).build()) },
                    enabled = state != LoadState.STATE_LOADING
                ) {
                    Text(text = "SUBMIT")
                }
            }
        }
    }
}
