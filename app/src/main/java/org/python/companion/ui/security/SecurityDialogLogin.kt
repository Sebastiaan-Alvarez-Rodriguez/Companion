package org.python.companion.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.python.security.SecurityActor
import org.python.security.SecurityType

@Composable
fun SecurityDialogLoginOptions(onNegativeClick: () -> Unit, onSetupClick: () -> Unit) {
    val defaultPadding = dimensionResource(id = org.python.companion.R.dimen.padding_default)
    val message = "In order to use this login method, it must be set-up first."
    Card(elevation = 8.dp, modifier = Modifier.padding(defaultPadding).wrapContentHeight(), shape = RoundedCornerShape(12.dp)) {
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
    bioState: SecurityBioState,
    passState: SecurityPassState,
    allowResetCalls: Boolean = true,
) {
    when(method) {
        SecurityActor.TYPE_PASS -> passState.Login(allowResetCalls = allowResetCalls)
        SecurityActor.TYPE_BIO -> bioState.Login()
    }
}