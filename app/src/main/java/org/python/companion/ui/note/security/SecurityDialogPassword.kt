package org.python.companion.ui.note.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.python.backend.security.PasswordVerificationToken
import org.python.backend.security.VerificationToken
import org.python.companion.R
import org.python.companion.support.LoadState
import org.python.companion.support.LoadingState
import org.python.companion.support.UiUtil.SimpleLoading
import org.python.companion.support.UiUtil.SimpleOk
import java.nio.ByteBuffer


class PasswordDialogMiniState(
    val state: MutableState<Int>,
    val stateMessage: MutableState<String?>,
    val pass: MutableState<String>,
    val passVisible: MutableState<Boolean>,
    open: MutableState<Boolean>
) : SecurityDialogState(open) {
    override fun open() {
        open.value = true
        state.value = LoadState.STATE_READY
    }

    override fun close() {
        open.value = false
        stateMessage.value = null
        pass.value = ""
    }

    @Composable
    override fun Dialog(
        onDismiss: () -> Unit,
        onNegativeClick: () -> Unit,
        onPositiveClick: (VerificationToken) -> Unit
    ) {
        SecurityPasswordDialog(
            onDismiss = onDismiss,
            onNegativeClick = onNegativeClick,
            onPositiveClick = onPositiveClick,
            state = this,
        )
    }

    companion object {
        @Composable
        fun rememberState(
            stateMessage: String? = null,
            @LoadingState state: Int = LoadState.STATE_OK,
            passVisible: Boolean = false,
            open: Boolean = false
        ) =
            remember(open) {
                PasswordDialogMiniState(
                    state = mutableStateOf(state),
                    stateMessage = mutableStateOf(stateMessage),
                    pass = mutableStateOf(""),
                    passVisible = mutableStateOf(passVisible),
                    open = mutableStateOf(open)
                )
            }
    }
}

@Composable
fun SecurityPasswordDialog(
    onDismiss: () -> Unit,
    onNegativeClick: () -> Unit,
    onPositiveClick: (PasswordVerificationToken) -> Unit,
    state: PasswordDialogMiniState
) {
    if (state.open.value)
        Dialog(onDismissRequest = onDismiss) {
            Card(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                when(state.state.value) {
                    LoadState.STATE_READY -> SecurityPasswordDialogReady(onNegativeClick, onPositiveClick, state)
                    LoadState.STATE_LOADING -> SimpleLoading()
                    LoadState.STATE_FAILED -> SecurityPasswordDialogReady(onNegativeClick, onPositiveClick, state)
                    LoadState.STATE_OK -> SimpleOk()
                }
            }
        }
}

@Composable
private fun SecurityPasswordDialogReady(
    onNegativeClick: () -> Unit,
    onPositiveClick: (PasswordVerificationToken) -> Unit,
    state: PasswordDialogMiniState
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    Column(modifier = Modifier.padding(defaultPadding)) {
        Text(
            text = "Please authenticate",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(defaultPadding)
        )
        Spacer(modifier = Modifier.height(defaultPadding))

        OutlinedTextField(
            isError = state.state.value == LoadState.STATE_FAILED,
            value = state.pass.value,
            onValueChange = { state.pass.value = it },
            label = { Text("Enter password...") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (state.passVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            trailingIcon = {
                val image = if (state.passVisible.value) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { state.passVisible.value = !state.passVisible.value }) {
                    Icon(imageVector = image, "")
                }
            }
        )
        val msgVal = state.stateMessage.value
        if (msgVal != null)
            Text(text = msgVal, fontSize = 8.sp)

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
                        ByteBuffer.wrap(state.pass.value.toByteArray(charset = Charsets.ISO_8859_1))
                    ).build()
                )
            }) {
                Text(text = "SUBMIT")
            }
        }
    }
}
