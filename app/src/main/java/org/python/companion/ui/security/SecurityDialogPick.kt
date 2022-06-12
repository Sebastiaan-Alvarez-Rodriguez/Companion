package org.python.companion.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Password
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.python.security.SecurityActor
import org.python.security.SecurityType
import org.python.companion.R
import org.python.companion.ui.theme.DarkColorPalette
import org.python.security.SecurityTypes

@Composable
fun SecurityDialogPick(
    headerText: String = "Select a method",
    onNegativeClick: () -> Unit,
    onPositiveClick: (Int) -> Unit,
    allowedMethods: Collection<@SecurityType Int> = SecurityTypes.toList(),
    showDisallowed: Boolean = false,
    disallowedReason: String? = null
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)

    val iconMap = mapOf<@SecurityType Int, Pair<ImageVector, String>>(
        Pair(SecurityActor.TYPE_PASS, Pair(Icons.Filled.Password, "Password")),
        Pair(SecurityActor.TYPE_BIO, Pair(Icons.Filled.Fingerprint, "Fingerprint"))
    )
    Card(elevation = 8.dp, modifier = Modifier.padding(defaultPadding).wrapContentHeight(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(defaultPadding)) {
            Text(text = headerText, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(defaultPadding))
            Spacer(modifier = Modifier.height(defaultPadding))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val lazyMap = if (showDisallowed) iconMap else iconMap.filter { item -> item.key in allowedMethods }
                for (entry in lazyMap) {
                    Card(elevation = 8.dp, shape = RoundedCornerShape(12.dp)) {
                        IconButton(
                            modifier = Modifier.padding(tinyPadding).size(32.dp).background(
                                color = if (entry.key in allowedMethods) DarkColorPalette.primary else DarkColorPalette.background
                            ),
                            onClick = { onPositiveClick(entry.key) },
                            enabled = entry.key in allowedMethods
                        ) {
                            Icon(entry.value.first, entry.value.second)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(defaultPadding))
            if (!disallowedReason.isNullOrEmpty()) {
                val prefix = if (showDisallowed) "Some methods are disabled:" else "Some methods are not shown:"
                Text(text = "$prefix $disallowedReason", modifier = Modifier.padding(horizontal = defaultPadding))
                Spacer(modifier = Modifier.height(defaultPadding))
            }
            TextButton(onClick = onNegativeClick) {
                Text(text = "CANCEL")
            }
        }
    }
}
