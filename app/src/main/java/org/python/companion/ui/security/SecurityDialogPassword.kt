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
import org.python.companion.support.UiUtil

@Composable
fun SecurityPasswordDialogContent(
    onNegativeClick: () -> Unit,
    onPositiveClick: (PasswordVerificationToken) -> Unit,
    onResetPasswordClick: () -> Unit,
    state: UiUtil.StateMiniState
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    var pass by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }

    Card(elevation = 8.dp, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(defaultPadding)) {
            Text(
                text = "Please authenticate",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(defaultPadding)
            )
            Spacer(modifier = Modifier.height(defaultPadding))

            OutlinedTextField(
                enabled = state.state.value != LoadState.STATE_LOADING,
                isError = state.state.value == LoadState.STATE_FAILED,
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Enter password...") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                trailingIcon = {
                    val image =
                        if (passVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passVisible = !passVisible }) {
                        Icon(imageVector = image, "")
                    }
                }
            )
            state.stateMessage.value.let {
                if (it != null)
                    Text(text = it, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(defaultPadding))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                when (state.state.value) {
                    LoadState.STATE_LOADING -> CircularProgressIndicator()
                    LoadState.STATE_OK -> Icon(imageVector = Icons.Filled.CheckCircle, "Ok")
                    else -> TextButton(onClick = onResetPasswordClick) {
                        Text(text = "RESET PASSWORD")
                    }
                }
                Row(horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onNegativeClick, enabled = state.state.value != LoadState.STATE_LOADING) {
                        Text(text = "CANCEL")
                    }
                    Spacer(modifier = Modifier.width(smallPadding))
                    TextButton(
                        onClick = { onPositiveClick(PasswordVerificationToken.PassBuilder().with(pass).build()) },
                        enabled = state.state.value != LoadState.STATE_LOADING
                    ) {
                        Text(text = "SUBMIT")
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityPasswordSetupDialogContent(
    onNegativeClick: () -> Unit,
    onPositiveClick: (PasswordVerificationToken) -> Unit,
    title: String,
    state: UiUtil.StateMiniState
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    var pass by remember { mutableStateOf("") }
    var repeatPass by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var passMatch by remember { mutableStateOf(false) }

    Card(elevation = 8.dp, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier
            .padding(defaultPadding)
            .fillMaxSize()) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(defaultPadding)
            )
            Spacer(modifier = Modifier.height(defaultPadding))

            OutlinedTextField(
                enabled = state.state.value != LoadState.STATE_LOADING,
                isError = state.state.value == LoadState.STATE_FAILED,
                value = pass,
                onValueChange = {
                    pass = it
                    passMatch = pass == repeatPass
                },
                label = { Text("Enter new password...") },
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
            OutlinedTextField(
                enabled = state.state.value != LoadState.STATE_LOADING,
                isError = state.state.value == LoadState.STATE_FAILED,
                value = repeatPass,
                onValueChange = {
                    repeatPass = it
                    passMatch = pass == repeatPass
                },
                label = { Text("Repeat new password...") },
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
            state.stateMessage.value.let {
                if (it != null)
                    Text(text = it, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(defaultPadding))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                when (state.state.value) {
                    LoadState.STATE_LOADING -> CircularProgressIndicator()
                    LoadState.STATE_OK -> Icon(imageVector = Icons.Filled.CheckCircle, "Ok")
                }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onNegativeClick) {
                        Text(text = "CANCEL")
                    }
                    Spacer(modifier = Modifier.width(smallPadding))
                    TextButton(
                        onClick = { onPositiveClick(PasswordVerificationToken.PassBuilder().with(pass).build()) },
                        enabled = passMatch
                    ) {
                        Text(text = "SUBMIT")
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityPasswordResetDialogContent(
    onNegativeClick: () -> Unit,
    onPickOtherMethodClick: () -> Unit,
    onDestructiveResetPasswordClick: () -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    Card(elevation = 8.dp, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(defaultPadding)) {
            Text(
                text = "Choose a reset method",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(defaultPadding)
            )
            Spacer(modifier = Modifier.height(defaultPadding))

            Column {
                TextButton(onClick = onDestructiveResetPasswordClick) {
                    Text(text = "DESTRUCT & RESET")
                }
                Spacer(modifier = Modifier.height(smallPadding))
                Text(
                    text = "Delete all secure notes and reset password. WARNING: All secure notes will be deleted and unrecoverable."
                )
            }
            Column {
                TextButton(onClick = onPickOtherMethodClick) {
                    Text(text = "PICK AUTH METHOD")
                }
                Spacer(modifier = Modifier.height(smallPadding))
                Text(
                    text = "Use another authentication method to login. Once logged in, you can reset the password."
                )
            }

            Spacer(modifier = Modifier.height(defaultPadding))

            Row(horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onNegativeClick) {
                    Text(text = "CANCEL")
                }
            }
        }
    }
}