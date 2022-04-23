package org.python.companion.ui.cactus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.python.companion.R


@Composable
fun CactusBody(onCactusClick: () -> Unit) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Column (
        modifier = Modifier.semantics { contentDescription = "Cactus Screen" }.padding(defaultPadding),
        verticalArrangement = Arrangement.spacedBy(defaultPadding),
    ) {
        Text(text = "Hello cactus")
    }
}