package org.python.companion.ui.settings.import_export

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import org.python.companion.R
import org.python.companion.ui.theme.DarkColorPalette


/**
 * Import/Export settings screen, where users can start import/export.
 * @param isExport If `true`, assumes export context. Assumes import context otherwise.
 * @param onStartClick Lambda to perform on start clicks.
 * @param onBackClick Lambda to perform on back clicks.
 */
@Composable
fun ImportExportScreenSettings(
    isExport: Boolean = true,
    onStartClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val animatedProgress = animateFloatAsState(
        targetValue = 1f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value

    Column(modifier = Modifier.fillMaxSize().padding(defaultPadding)) {
        ViewHeader(onBackClick = onBackClick)

        Spacer(Modifier.height(defaultPadding))

        CircularProgressIndicator(
            modifier = Modifier.weight(1f).aspectRatio(1f),
            color = DarkColorPalette.secondary,
            progress = animatedProgress
        )

        Spacer(Modifier.height(defaultPadding))

        Button(modifier = Modifier.fillMaxWidth(), onClick = onStartClick) {
            Text("Begin ${if (isExport) "export" else "import"}", modifier = Modifier.padding(defaultPadding))
        }
    }
}


@Composable
fun ImportExportExecutionScreen(
    progress: Float = 0f,
    detailsDescription: String? = null,
    onBackClick: () -> Unit
) {
    var detailsExpanded by remember { mutableStateOf(false) }
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value
    val reverseAnimatedProgress = animateFloatAsState(
        targetValue = 1f - progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Column(modifier = Modifier.fillMaxSize().padding(defaultPadding)) {
        ViewHeader(onBackClick = onBackClick)

        Spacer(Modifier.height(defaultPadding))

        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
            CircularProgressIndicator(
                modifier = Modifier.aspectRatio(1f).scale(scaleX = -1f, scaleY = 1f),
                color = DarkColorPalette.secondary,
                progress = reverseAnimatedProgress
            )
            CircularProgressIndicator(
                modifier = Modifier.aspectRatio(1f),
                color = DarkColorPalette.primary,
                progress = animatedProgress
            )
        }

        Spacer(Modifier.height(defaultPadding))

        Card(elevation = 5.dp) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { detailsExpanded = !detailsExpanded }) {
                        Text("Show details", modifier = Modifier.padding(defaultPadding))
                    }
                    IconButton(onClick = { detailsExpanded = !detailsExpanded }) {
                        Icon(
                            if (detailsExpanded) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = "${if (detailsExpanded) "Open" else "Close"} details"
                        )
                    }
                }
                if (detailsExpanded) {
                    Text(text = "(${String.format("%.2f", progress * 100)}%) ${detailsDescription ?: "Processing..."}", modifier = Modifier.padding(defaultPadding), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ViewHeader(onBackClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBackClick) {
            Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Back")
        }
    }
}