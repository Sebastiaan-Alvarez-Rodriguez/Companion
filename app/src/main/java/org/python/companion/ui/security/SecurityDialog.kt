package org.python.companion.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import org.python.backend.security.VerificationToken
import org.python.companion.support.UiUtil

abstract class SecurityDialogState(open: MutableState<Boolean>) : UiUtil.DialogMiniState(open) {
    @Composable
    abstract fun Dialog(
        onDismiss: () -> Unit,
        onNegativeClick: () -> Unit,
        onPositiveClick: (VerificationToken) -> Unit
    )
}

abstract class SecurityDialogPickState(open: MutableState<Boolean>) : UiUtil.DialogMiniState(open) {
    @Composable
    abstract fun Dialog(
        onDismiss: () -> Unit,
        onNegativeClick: () -> Unit,
        onPositiveClick: (Int) -> Unit
    )
}

abstract class SecurityDialogSetupState(open: MutableState<Boolean>) : UiUtil.DialogMiniState(open) {
    @Composable
    abstract fun Dialog(
        onDismiss: () -> Unit,
        onNegativeClick: () -> Unit,
        onPositiveClick: (VerificationToken) -> Unit
    )
}