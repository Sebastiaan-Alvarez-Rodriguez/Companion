package org.python.companion.ui.security

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
import org.python.companion.support.UiUtil
import java.nio.ByteBuffer


class PasswordSetupDialogMiniState(
    val state: MutableState<Int>,
    val stateMessage: MutableState<String?>,
    val pass: MutableState<String>,
    val match: MutableState<Boolean> = mutableStateOf(false),
    val passVisible: MutableState<Boolean>,
    open: MutableState<Boolean>
) : SecurityDialogSetupState(open) {

    override fun open() {
        open.value = true
    }

    override fun close() {
        open.value = false
    }

    @Composable
    override fun Dialog(
        onDismiss: () -> Unit,
        onNegativeClick: () -> Unit,
        onPositiveClick: (VerificationToken) -> Unit
    ) {
        SecurityDialogSetupPassword(
            onDismiss = onDismiss,
            onNegativeClick = onNegativeClick,
            onPositiveClick = onPositiveClick,
            state = this,
        )
    }

    companion object {
        @Composable
        fun rememberState(
            @LoadingState state: Int = LoadState.STATE_OK,
            stateMessage: String? = null,
            passVisible: Boolean = false,
            open: Boolean = false
        ) = remember(open) {
            PasswordSetupDialogMiniState(
                state = mutableStateOf(state),
                stateMessage = mutableStateOf(stateMessage),
                pass = mutableStateOf(""),
                match = mutableStateOf(false),
                passVisible = mutableStateOf(passVisible),
                open = mutableStateOf(open)
            )
        }
    }
}
@Composable
fun SecurityDialogSetupPasswordContent(
    onNegativeClick: () -> Unit,
    onPositiveClick: (PasswordVerificationToken) -> Unit,
//    state: PasswordSetupDialogMiniState
) {
    Card(
        elevation = 8.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        when(state.state.value) {
            LoadState.STATE_READY,
            LoadState.STATE_FAILED -> SecurityDialogSetupPasswordReady(onNegativeClick, onPositiveClick, state)
            LoadState.STATE_LOADING -> UiUtil.SimpleLoading()
            LoadState.STATE_OK -> UiUtil.SimpleOk()
        }
    }
}

@Composable
fun SecurityDialogSetupPasswordReady(
    onNegativeClick: () -> Unit,
    onPositiveClick: (PasswordVerificationToken) -> Unit,
    state: PasswordSetupDialogMiniState
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    val repeatPassword = remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(defaultPadding)) {
        Text(
            text = "Setup a method to authenticate",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(defaultPadding)
        )
        Spacer(modifier = Modifier.height(defaultPadding))

        OutlinedTextField(
            isError = state.state.value == LoadState.STATE_FAILED,
            value = state.pass.value,
            onValueChange = { state.pass.value = it },
            label = { Text("Enter new password...") },
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
        OutlinedTextField(
            isError = state.state.value == LoadState.STATE_FAILED,
            value = repeatPassword.value,
            onValueChange = {
                repeatPassword.value = it
                state.match.value = repeatPassword.value == state.pass.value
            },
            label = { Text("Repeat new password...") },
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
            TextButton(
                onClick = {
                    onPositiveClick(PasswordVerificationToken.PassBuilder().with(
                        ByteBuffer.wrap(state.pass.value.toByteArray(charset = Charsets.ISO_8859_1))).build()
                    )
                },
                enabled = state.match.value
            ) {
                Text(text = "SUBMIT")
            }
        }
    }
}