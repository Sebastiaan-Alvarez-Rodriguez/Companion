package org.python.companion.ui.note.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Password
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.python.backend.security.SecurityActor
import org.python.companion.R
import org.python.companion.support.LoadState
import org.python.companion.support.LoadingState

class SecurityDialogPickMiniState(
    val state: MutableState<Int>,
    val stateMessage: MutableState<String?>,
    open: MutableState<Boolean>
) : SecurityDialogPickState(open) {
    override fun open() {
        open.value = true
        state.value = LoadState.STATE_READY
    }

    override fun close() {
        open.value = false
        stateMessage.value = null
    }

    @Composable
    override fun Dialog(
        onDismiss: () -> Unit,
        onNegativeClick: () -> Unit,
        onPositiveClick: (Int) -> Unit
    ) {
        SecurityPickDialog(
            onDismiss = onDismiss,
            onNegativeClick = onNegativeClick,
            onPositiveClick = onPositiveClick,
            headerText = stateMessage.value,
        )
    }

    companion object {
        @Composable
        fun rememberState(
            stateMessage: String? = null,
            @LoadingState state: Int = LoadState.STATE_OK,
            open: Boolean = false
        ) = remember(open) {
                SecurityDialogPickMiniState(
                    state = mutableStateOf(state),
                    stateMessage = mutableStateOf(stateMessage),
                    open = mutableStateOf(open)
                )
            }
    }
}

@Composable
fun SecurityPickDialog(
    headerText: String?,
    onDismiss: () -> Unit,
    onNegativeClick: () -> Unit,
    onPositiveClick: (Int) -> Unit,
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
                    text = headerText ?: "Select a method to login",
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
                        onClick = { onPositiveClick(SecurityActor.TYPE_PASS) }) {
                        Icon(Icons.Filled.Password, "Password login")
                    }
                    Spacer(modifier = Modifier.width(defaultPadding))
                    IconButton(
                        modifier = Modifier.padding(tinyPadding),
                        onClick = { onPositiveClick(SecurityActor.TYPE_BIO) }) {
                        Icon(Icons.Filled.Fingerprint, "Biometric login")
                    }
                }
                Spacer(modifier = Modifier.height(defaultPadding))
                TextButton(onClick = onNegativeClick) {
                    Text(text = "CANCEL")
                }
            }
        }
    }
}
