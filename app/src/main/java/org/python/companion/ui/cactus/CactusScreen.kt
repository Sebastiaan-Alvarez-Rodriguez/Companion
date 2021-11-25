package org.python.companion.ui.cactus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun CactusBody(onCactusClick: () -> Unit) {
//    val scrollState = rememberScrollState()
    val defaultPadding: Dp = 10.dp //TODO: Why does this not work: dimensionResource(id = org.python.companion.R.dimen.padding_default)
    Column (
        modifier = Modifier
//            .verticalScroll(scrollState)
            .semantics { contentDescription = "Cactus Screen" }
            .padding(defaultPadding),
        verticalArrangement = Arrangement.spacedBy(defaultPadding),
    ) {
        Text(text = "Hello cactus")
    }
}