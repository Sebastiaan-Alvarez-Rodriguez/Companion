package org.python.companion.ui.security

import android.content.Context
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.python.companion.R
import org.python.security.PasswordVerificationToken

@Composable
fun SecurityPasswordDialog(
    saltContext: Context,
    onNegativeClick: () -> Unit,
    onPositiveClick: (PasswordVerificationToken) -> Unit,
    onResetPasswordClick: (() -> Unit)? = null,
    errorMessage: String? = null
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

            errorMessage?.let {
                Spacer(modifier = Modifier.height(defaultPadding))
                Text(text = it, style = TextStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
            }
            Spacer(modifier = Modifier.height(defaultPadding))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                onResetPasswordClick?.let {
                    TextButton(onClick = it) {
                        Text(text = "RESET PASSWORD")
                    }
                }

                Row(horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onNegativeClick) {
                        Text(text = "CANCEL")
                    }
                    Spacer(modifier = Modifier.width(smallPadding))
                    TextButton(onClick = {
                        onPositiveClick(PasswordVerificationToken.PassBuilder().with(pass, storedSaltContext = saltContext).build())
                    }) {
                        Text(text = "SUBMIT")
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityPassDialogSetup(
    onNegativeClick: () -> Unit,
    onPositiveClick: (PasswordVerificationToken) -> Unit,
    title: String,
    errorMessage: String? = null
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    var pass by remember { mutableStateOf("") }
    var repeatPass by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var passMatch by remember { mutableStateOf(false) }

    Card(elevation = 8.dp, modifier = Modifier.padding(defaultPadding).wrapContentHeight(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(defaultPadding).fillMaxWidth()) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(defaultPadding)
            )
            Spacer(modifier = Modifier.height(defaultPadding))

            OutlinedTextField(
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

            errorMessage?.let {
                Spacer(modifier = Modifier.height(defaultPadding))
                Text(text = it, style = TextStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
            }

            Spacer(modifier = Modifier.height(defaultPadding))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onNegativeClick) {
                    Text(text = "CANCEL")
                }
                Spacer(modifier = Modifier.width(smallPadding))
                TextButton(
                    onClick = { onPositiveClick(PasswordVerificationToken.PassBuilder().with(pass, salt = null).build()) },
                    enabled = passMatch
                ) {
                    Text(text = "SUBMIT")
                }
            }
        }
    }
}