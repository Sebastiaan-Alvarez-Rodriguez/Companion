package org.python.companion.ui.settings.import_export

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import org.python.companion.R
import org.python.companion.support.UiUtil
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
    path: String? = null,
    password: String,
    onStartClick: () -> Unit,
    onLocationSelectClick: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)
    val animatedProgress = animateFloatAsState(
        targetValue = 1f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(defaultPadding)) {
        ViewHeader(onBackClick = onBackClick)

        Spacer(Modifier.height(defaultPadding))

        CircularProgressIndicator(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            color = DarkColorPalette.secondary,
            progress = animatedProgress
        )

        Spacer(Modifier.height(defaultPadding))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.align(Alignment.CenterHorizontally).padding(defaultPadding)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = path ?: "Please select a path...")
                    IconButton(onClick = onLocationSelectClick) {
                        Icon(imageVector = Icons.Outlined.FileOpen, contentDescription = "Pick a file")
                    }
                }
                Spacer(Modifier.height(smallPadding))
                Text("pick a path to place the backup.")
            }
        }

        Spacer(Modifier.height(defaultPadding))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(defaultPadding)) {
                UiUtil.OutlinedPasswordField(
                    modifier = Modifier.fillMaxWidth(),
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Enter a backup password") }
                )
                Spacer(Modifier.height(smallPadding))
                Text(text = "Pick a password for this backup." +
                        "All data will be encrypted using this password." +
                        "You need this password to import the data, so remember it well."
                )
            }
        }

        Spacer(Modifier.height(defaultPadding))

        Button(modifier = Modifier.fillMaxWidth(), onClick = onStartClick) {
            Text("Begin ${if (isExport) "export" else "import"}", modifier = Modifier.padding(defaultPadding))
        }
    }
}


@Composable
fun ImportExportExecutionScreen(
    progress: List<Float> = listOf(0f),
    detailsDescription: String? = null,
    onBackClick: () -> Unit
) {
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(defaultPadding)) {
        ViewHeader(onBackClick = onBackClick)

        Spacer(Modifier.height(defaultPadding))

        NestedCircularProgressIndicator(progress)

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
                    Text(text = detailsDescription ?: "Processing...", modifier = Modifier.padding(defaultPadding), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun NestedCircularProgressIndicator(progresses: List<Float>) {
    val scaleDecrease = 0.2f
    val scales = (1..progresses.size).map {idx -> idx * scaleDecrease}
    Box(modifier = Modifier.aspectRatio(1f)) {// had .weight(1f) once
        for ((progress, scale) in progresses.zip(scales)) {
            val animatedProgress = animateFloatAsState(
                targetValue = progress,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
            ).value

            CircularProgressIndicator(
                modifier = Modifier
                    .aspectRatio(1f)
                    .scale(scale)
                    .align(Alignment.Center),
                color = DarkColorPalette.primary,
                progress = animatedProgress
            )
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