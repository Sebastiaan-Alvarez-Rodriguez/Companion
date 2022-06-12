package org.python.companion.ui.security

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import org.python.companion.R
import org.python.companion.ui.security.SecurityState.Companion.switchActor
import org.python.companion.viewmodels.SecurityViewModel
import org.python.security.SecurityActor
import org.python.security.SecurityType

@Composable
fun SecurityDialogReset(
    onNegativeClick: () -> Unit,
    onDestroyClick: () -> Unit,
    onLoginClick: () -> Unit,
    loginMethods: List<String>,
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val message =
        if (loginMethods.size > 1) // There is only 1 method and we want to reset it --> can only destroy
            """Detected set-up login methods: ${loginMethods}.
In order to reset, login using any of these methods.

Alternatively, if you forgot all ways to login, you can reset the security system, destroying all secure notes PERMANENTLY.""".trimIndent()
        else
            "If you forgot all ways to login, you can reset the security system destroying all secure notes PERMANENTLY."
    Card(elevation = 8.dp, modifier = Modifier.padding(defaultPadding).wrapContentHeight(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(defaultPadding)) {
            Text(text = "Reset Options", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(defaultPadding))
            Spacer(modifier = Modifier.height(defaultPadding))

            Text(text = message, modifier = Modifier.padding(defaultPadding))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {

                TextButton(onClick = onNegativeClick) {
                    Text(text = "CANCEL")
                }
                Spacer(modifier = Modifier.width(defaultPadding))
                TextButton(onClick = onDestroyClick) {
                    Text(text = "RESET SECURITY", style = TextStyle(color = Color.Red, fontStyle = FontStyle.Italic))
                }
                if (loginMethods.size > 1) {
                    Spacer(modifier = Modifier.width(defaultPadding))
                    TextButton(onClick = onLoginClick) {
                        Text(text = "LOGIN")
                    }
                }
            }
        }
    }
}

/** Shows specific reset UI for setting up given security type. */
@Composable
fun SecurityDialogResetSpecific(
    @SecurityType method: Int,
    securityViewModel: SecurityViewModel,
    navController: NavHostController
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { navController.navigateUp() }

    when(method) {
        SecurityActor.TYPE_PASS -> {
            if (!switchActor(securityViewModel.securityActor, SecurityActor.TYPE_PASS))
                return
            require(securityViewModel.securityActor.canReset())
            SecurityPassState.DoSetCredentialsReset(securityViewModel, navController)
        }
        SecurityActor.TYPE_BIO -> {
            if (!switchActor(securityViewModel.securityActor, SecurityActor.TYPE_BIO))
                return
            SecurityBioDialogReset(
                onNegativeClick = { navController.navigateUp() },
                onPositiveClick = { launcher.launch(SecurityBioState.createSecuritySettingsIntent()) }
            )
        }
    }
}
