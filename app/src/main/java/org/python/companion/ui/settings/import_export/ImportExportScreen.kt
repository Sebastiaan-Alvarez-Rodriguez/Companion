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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import org.python.companion.R
import org.python.companion.support.UiUtil
import org.python.companion.ui.theme.DarkColorPalette
import org.python.exim.EximUtil


@Composable
fun ImportExportScreenSettings(
    progressContent: @Composable () -> Unit,
    subContent: @Composable () -> Unit,
    onBackClick: () -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val scrollState = rememberScrollState()

    Column(modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(scrollState)
        .padding(defaultPadding)) {
        ViewHeader(onBackClick = onBackClick)

        Spacer(Modifier.height(defaultPadding))

        progressContent()

        Spacer(Modifier.height(defaultPadding*4))

        subContent()
    }
}

@Composable
fun PickFileCard(
    path: String? = null,
    explanationText: String? = null,
    pathError: String? = null,
    onLocationSelectClick: () -> Unit,
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(defaultPadding)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = path ?: "Please select a path...", color = if (pathError != null) Color.Red else Color.Unspecified)
                IconButton(onClick = onLocationSelectClick) {
                    Icon(imageVector = Icons.Outlined.FileOpen, contentDescription = "Pick a file")
                }
            }
            explanationText?.let {
                Spacer(Modifier.height(smallPadding))
                Text(explanationText)
            }
            if (pathError != null) {
                Spacer(Modifier.height(smallPadding))
                Text(text = pathError, color = Color.Red)
            }
        }
    }
}

@Composable
fun ImportPasswordCard(
    password: String,
    passwordError: String? = null,
    onPasswordChange: (String) -> Unit
) {
    ImportExportPasswordCard(
        password = password,
        hintText = "Enter backup password",
        explanationText = "Enter the same password as was set for exporting " +
                "to decrypt the data.",
        passwordError = passwordError,
        onPasswordChange = onPasswordChange
    )
}

@Composable
fun ExportPasswordCard(
    password: String,
    passwordError: String? = null,
    onPasswordChange: (String) -> Unit
) {
    ImportExportPasswordCard(
        password = password,
        hintText = "Enter a backup password",
        explanationText = "Pick a password for this backup." +
                "All data will be encrypted using this password." +
                "You need this password to import the data, so remember it well.",
        passwordError = passwordError,
        onPasswordChange = onPasswordChange
    )
}

@Composable
fun ImportExportPasswordCard(
    password: String,
    hintText: String,
    explanationText: String? = null,
    passwordError: String? = null,
    onPasswordChange: (String) -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(defaultPadding)) {
            UiUtil.OutlinedPasswordField(
                modifier = Modifier.fillMaxWidth(),
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(hintText) },
                isError = passwordError != null
            )
            Spacer(Modifier.height(smallPadding))
            explanationText?.let {
                Text(text = it)
            }
            if (passwordError != null) {
                Spacer(Modifier.height(smallPadding))
                Text(text = passwordError, color = Color.Red)
            }
        }
    }
}

@Composable
fun ImportMergeStrategyCard(mergeStrategy: EximUtil.MergeStrategy, onMergeStrategyChange: (EximUtil.MergeStrategy) -> Unit) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    val explanations = mapOf(
        EximUtil.MergeStrategy.DELETE_ALL_BEFORE to "Keep only imported notes, categories, delete all local content",
        EximUtil.MergeStrategy.SKIP_ON_CONFLICT to "Skip conflicting notes, categories",
        EximUtil.MergeStrategy.OVERRIDE_ON_CONFLICT to "Override conflicting notes, categories"
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(defaultPadding)) {
            Text("Merge strategy:")
            EximUtil.MergeStrategy.values().forEach {
                Row(modifier = Modifier.fillMaxWidth()) {
                    RadioButton(selected = it == mergeStrategy, onClick = { onMergeStrategyChange(it) })
                    Text(
                        text = explanations.getValue(it),
                        style = MaterialTheme.typography.body1.merge()
                    )
                }
                Spacer(Modifier.height(smallPadding))
            }
        }
    }
}

@Composable
fun DetailsCard(detailsDescription: String? = null) {
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Card(elevation = 5.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

/**
 * Creates a series of circular progress bars inside each other.
 * @param progresses Progresses to create progress bars for. Any negative value creates an indeterminate progress bar.
 * @param func Extra layout function.
 */
@Composable
fun NestedCircularProgressIndicator(progresses: List<Float>, func: (@Composable BoxScope.() -> Unit)? = null) {
    val scaleDecrease = 0.1f
    val scales = (1..progresses.size).map { idx -> 1f-(idx * scaleDecrease) }
    Box(modifier = Modifier.aspectRatio(1f)) {
        for ((progress, scale) in progresses.zip(scales)) {
            if (progress >= 0f) {
                val animatedProgress = animateFloatAsState(
                    targetValue = progress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                ).value

                CircularProgressIndicator(
                    modifier = Modifier.aspectRatio(1f).scale(scale).align(Alignment.Center),
                    color = DarkColorPalette.primary,
                    progress = animatedProgress
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.aspectRatio(1f).scale(scale).align(Alignment.Center),
                    color = DarkColorPalette.primary
                )
            }
        }
        if (func != null)
            func()
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