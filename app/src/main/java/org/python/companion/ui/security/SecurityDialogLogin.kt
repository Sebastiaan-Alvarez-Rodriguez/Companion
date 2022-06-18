package org.python.companion.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.python.companion.ui.security.SecurityState.Companion.switchActor
import org.python.companion.viewmodels.SecurityViewModel
import org.python.datacomm.ResultType
import org.python.security.SecurityActor
import org.python.security.SecurityType

@Composable
fun SecurityDialogLoginOptions(onNegativeClick: () -> Unit, onSetupClick: () -> Unit) {
    val defaultPadding = dimensionResource(id = org.python.companion.R.dimen.padding_default)
    val message = "In order to use this login method, it must be set-up first."
    Card(elevation = 8.dp, modifier = Modifier
        .padding(defaultPadding)
        .wrapContentHeight(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(defaultPadding)) {
            Text(text = "Login Options", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(defaultPadding))
            Spacer(modifier = Modifier.height(defaultPadding))

            Text(text = message, modifier = Modifier.padding(defaultPadding))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onNegativeClick) {
                    Text(text = "CANCEL")
                }
                Spacer(modifier = Modifier.width(defaultPadding))
                TextButton(onClick = onSetupClick) {
                    Text(text = "SETUP")
                }
            }
        }
    }
}

@Composable
fun SecurityDialogLoginSpecific(
    @SecurityType method: Int,
    securityViewModel: SecurityViewModel,
    scaffoldState: ScaffoldState,
    allowResetCalls: Boolean = true,
    navController: NavHostController
) {
    when(method) {
        SecurityActor.TYPE_PASS -> {
            if (!switchActor(securityViewModel.securityActor, SecurityActor.TYPE_PASS))
                return
            require(securityViewModel.securityActor.canLogin())

            val clearance by securityViewModel.securityActor.clearance.collectAsState()
            if (clearance > 0)
                navController.navigateUp()

            var errorMessage: String? by remember { mutableStateOf(null) }

            SecurityPasswordDialog(
                saltContext = LocalContext.current,
                onNegativeClick = { navController.navigateUp() },
                onPositiveClick = { token ->
                    securityViewModel.viewModelScope.launch {
                        val msg = securityViewModel.securityActor.verify(token)
                        if (msg.type != ResultType.SUCCESS) {
                            errorMessage = msg.message ?: "There was a problem setting up a new password."
                        }
                    }
                },
                onResetPasswordClick = if (allowResetCalls) {
                    { SecurityState.navigateToReset(SecurityActor.TYPE_PASS, navController) }
                } else
                    null,
                errorMessage = errorMessage
            )
        }
        SecurityActor.TYPE_BIO -> {
            if (!switchActor(securityViewModel.securityActor, SecurityActor.TYPE_BIO))
                return
            require(securityViewModel.securityActor.canLogin())

            val securityLevel by securityViewModel.securityActor.clearance.collectAsState()
            if (securityLevel > 0)
                navController.navigateUp()

            LaunchedEffect(true) {
                val msgSec = securityViewModel.securityActor.verify(null)
                if (msgSec.type != ResultType.SUCCESS) {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = msgSec.message ?: "There was a login problem.",
                        duration = SnackbarDuration.Short
                    )
                    navController.navigateUp()
                }
            }
        }
    }
}