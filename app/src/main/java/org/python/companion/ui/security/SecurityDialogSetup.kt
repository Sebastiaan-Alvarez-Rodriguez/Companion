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
import org.python.companion.R
import org.python.security.SecurityActor
import org.python.security.SecurityType

@Composable
fun SecurityDialogSetupOptions(
    onNegativeClick: () -> Unit,
    onLoginClick: () -> Unit,
    loginMethods: List<String>,
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val message = """Detected set-up login methods: ${loginMethods}.
In order to setup another method, you must first login using any of these methods.

Alternatively, if you forgot all ways to login, you can reset the security system, (see options > security > reset)""".trimIndent()
    Card(elevation = 8.dp, modifier = Modifier
        .padding(defaultPadding)
        .wrapContentHeight(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(defaultPadding)) {
            Text(text = "Setup Options", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(defaultPadding))
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
                TextButton(onClick = onLoginClick) {
                    Text(text = "LOGIN")
                }
            }
        }
    }
}

/** Shows specific setup UI for setting up given security type. */
@Composable
fun SecurityDialogSetupSpecific(
    @SecurityType method: Int,
    bioState: SecurityBioState,
    passState: SecurityPassState
) {
    when(method) {
        SecurityActor.TYPE_PASS -> passState.Setup()
        SecurityActor.TYPE_BIO -> bioState.Setup()
    }
}