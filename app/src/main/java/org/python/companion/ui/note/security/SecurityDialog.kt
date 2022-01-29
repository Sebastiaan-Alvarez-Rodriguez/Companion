package org.python.companion.ui.note.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import org.python.backend.security.SecurityActor
import org.python.backend.security.VerificationToken
import org.python.companion.support.UiUtil


data class SecurityDialogStateHolder(
    var dialogStates: Map<Int, SecurityDialogState>,
    var setupStates: Map<Int, SecurityDialogSetupState>
) {
    companion object {
        @Composable
        fun create() = SecurityDialogStateHolder(
            dialogStates = mapOf(
                SecurityActor.TYPE_PASS to PasswordDialogMiniState.rememberState(),
            ),
            setupStates = mapOf(
                SecurityActor.TYPE_PASS to PasswordSetupDialogMiniState.rememberState()
            )
        )
    }
}

abstract class SecurityDialogState(open: MutableState<Boolean>) : UiUtil.DialogMiniState(open) {
    @Composable
    abstract fun Dialog(
        onDismiss: () -> Unit,
        onNegativeClick: () -> Unit,
        onPositiveClick: (VerificationToken) -> Unit
    )
}