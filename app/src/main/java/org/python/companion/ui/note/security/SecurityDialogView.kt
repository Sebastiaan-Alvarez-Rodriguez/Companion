package org.python.companion.ui.note.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.python.backend.security.SecurityActor
import org.python.backend.security.VerificationMessage
import org.python.backend.security.VerificationToken
import org.python.companion.AuthenticationState
import org.python.companion.support.LoadState
import org.python.companion.viewmodels.NoteViewModel


object SecurityDialogView {
    @Composable
    fun DialogSetup(onDismiss: () -> Unit, onPositiveClick: (Int) -> Unit) {
        val pickedState = remember { mutableStateOf(SecurityActor.TYPE_UNDEFINED) }
        when (pickedState.value) {
            SecurityActor.TYPE_UNDEFINED -> DialogPick(
                onDismiss = onDismiss,
                pickedActorType = pickedState
            )
            SecurityActor.TYPE_BIO -> { onPositiveClick(SecurityActor.TYPE_BIO) }
            SecurityActor.TYPE_PASS -> DialogSetupPassword(
                onDismiss = onDismiss,
                onPositiveClick = { onPositiveClick(SecurityActor.TYPE_PASS) }
            )
        }
    }

    /**
     * Picks an authentication type and changes the mutable state.
     * Changes state only once per user click.
     * @param pickedActorType Mutable state to change on pick.
     * @param onDismiss dismiss events are forwarded here.
     */
    @Composable
    fun DialogPick(pickedActorType: MutableState<Int>, onDismiss: () -> Unit) {
        val securityPickerState = SecurityDialogPickMiniState.rememberState()
        securityPickerState.Dialog(
            onDismiss = {
                onDismiss()
                securityPickerState.close()
            },
            onNegativeClick = {
                onDismiss()
                securityPickerState.close()
            },
            onPositiveClick = { type ->
                pickedActorType.value = type
                securityPickerState.close()
            }
        )
        securityPickerState.open()
    }

    @Composable
    fun DialogSetupPassword(onDismiss: () -> Unit, onPositiveClick: () -> Unit) {
        // TODO: Verify whether a password has been set already.
        //  If so, first login, or 'forget password -> erase secure notes'.
        val passwordSetupDialogState = PasswordSetupDialogMiniState.rememberState()
        passwordSetupDialogState.Dialog(
            onDismiss = {
                passwordSetupDialogState.close()
                onDismiss()
            },
            onNegativeClick = {
                passwordSetupDialogState.close()
                onDismiss()
            },
            onPositiveClick = { token -> // TODO("Store token")
                    onPositiveClick()
            }
        )
        passwordSetupDialogState.open()
    }

    ///// Verification dialogs /////

    @Composable
    fun DialogBio(authState: AuthenticationState, viewmodel: NoteViewModel) { viewmodel.with { authState.authenticate() } }

    @Composable
    fun DialogPassword(authState: AuthenticationState, viewmodel: NoteViewModel, onDismiss: () -> Unit, onSuccess: (VerificationToken) -> Unit) {
        val passwordDialogMiniState = PasswordDialogMiniState.rememberState()
        passwordDialogMiniState.Dialog(
            onDismiss = {
                onDismiss()
                passwordDialogMiniState.close()
            },
            onNegativeClick = {
                onDismiss()
                passwordDialogMiniState.close()
            },
            onPositiveClick = {
                viewmodel.with {
                    passwordDialogMiniState.state.value = LoadState.STATE_LOADING
                    val msg = authState.authenticate(it)
                    passwordDialogMiniState.state.value = when (msg.type) {
                        VerificationMessage.SEC_CORRECT -> {
                            passwordDialogMiniState.close()
                            onSuccess(it)
                            LoadState.STATE_OK
                        }
                        else -> {
                            passwordDialogMiniState.stateMessage.value = msg.body?.userMessage
                            LoadState.STATE_FAILED
                        }
                    }
                }
            }
        )
    }
}