package org.python.companion.ui.note.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import org.python.backend.security.VerificationToken
import org.python.companion.support.UiUtil


abstract class SecurityDialogSetupState(open: MutableState<Boolean>) : UiUtil.DialogMiniState(open) {
    @Composable
    abstract fun Dialog(
        onDismiss: () -> Unit,
        onNegativeClick: () -> Unit,
        onPositiveClick: (VerificationToken) -> Unit
    )
}