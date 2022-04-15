package org.python.companion.support

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.python.companion.R
import kotlin.math.roundToInt

/** Simple enum representing loading state of asynchronously loading objects. */
enum class LoadingState {
    LOADING, READY, FAILED, OK
}

object UiUtil {
    @Composable
    fun LabelledCheckBox(checked: Boolean, label: AnnotatedString, onCheckedChange: (Boolean) -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(checked) }
            ).semantics(mergeDescendants = true) {}) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange, interactionSource = interactionSource)
            Text(text = label)
        }
    }
    @Composable
    fun LabelledCheckBox(checked: Boolean, label: String, onCheckedChange: (Boolean) -> Unit) =
        LabelledCheckBox(checked = checked, label = AnnotatedString(label), onCheckedChange = onCheckedChange)

    @Composable
    fun SimpleLoading() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }

    @Composable
    fun SimpleProblem(message: String, prefix: String = "Error", icon: ImageVector = Icons.Outlined.ErrorOutline) {
        val defaultPadding = dimensionResource(id = R.dimen.padding_default)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Error",
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(defaultPadding))
                Text("[$prefix]: $message")
            }
        }
    }

    @Composable
    fun SimpleOk() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.CheckCircle, "Ok")
        }
    }

    @Composable
    fun SimpleText(text: String) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text)
        }
    }

    @Composable
    fun SimpleDialogBinary(
        message: String,
        negativeText: String = "CANCEL",
        positiveText: String = "OK",
        onDismiss: () -> Unit,
        onNegativeClick: () -> Unit,
        onPositiveClick: () -> Unit,
        showableObjectFunc: @Composable () -> Unit
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Card(elevation = 8.dp, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = message,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    showableObjectFunc()

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        TextButton(onClick = onNegativeClick) {
                            Text(text = negativeText.uppercase())
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = onPositiveClick) {
                            Text(text = positiveText.uppercase())
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SimpleColorPick(color: android.graphics.Color, onColorUpdate: (android.graphics.Color) -> Unit) {
        val red = color.red()
        val green = color.green()
        val blue = color.blue()

        val defaultPadding = dimensionResource(id = R.dimen.padding_default)
        val smallPadding = dimensionResource(id = R.dimen.padding_small)
        val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)

        Column(modifier = Modifier.padding(defaultPadding)) {
            Text(
                text = "Select Color",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(defaultPadding))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column {
                    Text(text = "Red ${(red*255).toInt()}")
                    Slider(
                        value = red,
                        onValueChange = { onColorUpdate(android.graphics.Color.valueOf(it, green, blue)) },
                        onValueChangeFinished = {}
                    )
                    Spacer(modifier = Modifier.height(tinyPadding))

                    Text(text = "Green ${(green*255).toInt()}")
                    Slider(
                        value = green,
                        onValueChange = { onColorUpdate(android.graphics.Color.valueOf(red, it, blue)) },
                        onValueChangeFinished = {}
                    )
                    Spacer(modifier = Modifier.height(tinyPadding))

                    Text(text = "Blue ${(blue*255).toInt()}")
                    Slider(
                        value = blue,
                        onValueChange = { onColorUpdate(android.graphics.Color.valueOf(red, green, it)) },
                        onValueChangeFinished = {}
                    )

                    Spacer(modifier = Modifier.height(smallPadding))
                    Surface(
                        border = BorderStroke(1.dp, Color.DarkGray),
                        color = Color(red, green, blue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {}
                }
            }
        }
    }

    @Composable
    fun GenericListHeader(items: Collection<@Composable RowScope.() -> Unit>) {
        Card(elevation = 5.dp) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (item in items) {
                    item()
                }
            }
        }
    }

    @Composable
    fun <T : Any> GenericList(
        prefix: @Composable (LazyItemScope.() -> Unit)? = null,
        items: Flow<PagingData<T>>,
        isLoading: Boolean,
        showItemFunc: @Composable LazyItemScope.(item: T) -> Unit,
        fab: (@Composable BoxScope.() -> Unit)? = null
    ) {
        //    TODO: Maybe add sticky headers: https://developer.android.com/jetpack/compose/lists
        val defaultPadding = dimensionResource(id = R.dimen.padding_default)
        val lazyCollectItems: LazyPagingItems<T> = items.collectAsLazyPagingItems()
        val listState: LazyListState = rememberLazyListState()

        Box(Modifier.fillMaxSize()) {
            when {
                isLoading -> SimpleLoading()
                lazyCollectItems.itemCount == 0 && prefix == null -> SimpleText("Nothing here yet")
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { contentDescription = "Item" },
                        verticalArrangement = Arrangement.spacedBy(defaultPadding),
                        state = listState,
                    ) {
                        if (prefix != null) {
                            item {
                                prefix()
                            }
                        }
                        items(items = lazyCollectItems) { item ->
                            if (item != null)
                                showItemFunc(item)
                        }
                    }
                }
            }
            if (fab != null)
                fab()
        }
    }

    @Composable
    fun BoxScope.SimpleFAB(onClick: () -> Unit) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Text("+")
        }
    }

    @Composable
    fun SimpleSearch(
        searchExpand: Boolean,
        searchText: String,
        onPreSearch: () -> Unit,
        onQueryUpdate: (String) -> Unit,
        onPostSearch: () -> Unit,
    ) {
        val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)

        if (!searchExpand) {
            IconButton(modifier = Modifier.padding(tinyPadding), onClick = {
                onPreSearch()
            }) {
                Icon(Icons.Filled.Search, "Search")
            }
        } else {
            TextField(
                value = searchText,
                onValueChange = onQueryUpdate,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", modifier = Modifier.padding(tinyPadding)) },
                trailingIcon = {
                    if (searchText != "") {
                        IconButton( onClick = { onQueryUpdate("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "reset", modifier = Modifier.padding(tinyPadding))
                        }
                    }
                })
        }

        BackHandler(searchExpand) {
            onPostSearch()
        }
    }

    @Composable
    fun SimpleScrollableText(
        text: AnnotatedString,
        modifier: Modifier,
        scrollState: ScrollState
    ): (Int) -> Unit {
        val coroutineScope = rememberCoroutineScope()

        val searchResultPositions = /* TODO: get title scrollable positions */ text.spanStyles.map { it.start } // Char offsets for each search result

        var contentTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) } // Layout rendering collector

        val executeScroll: (Int) -> Unit = { spanIndex ->
            coroutineScope.launch {
                val charOffset = searchResultPositions[spanIndex]
                val renderedLineOffset = contentTextLayoutResult!!.getLineForOffset(charOffset)
                val pointOffset = contentTextLayoutResult!!.getLineBottom(renderedLineOffset).roundToInt()
                scrollState.animateScrollTo(pointOffset)
            }
        }
        Text(text = text, modifier = modifier, onTextLayout = { layout -> contentTextLayoutResult = layout })
        return executeScroll

    }


    class UIUtilState(private val navController: NavHostController) {

        fun NavGraphBuilder.utilGraph() {
            navigation(startDestination = uiutilDestination, route = "uiutil") {
                composable(uiutilDestination) {}

                dialog(route = "${uiutilDestination}/binary?message={message}&negativeText={negativeText}&positiveText={positiveText}") { entry ->
                    SimpleDialogBinary(
                        message = entry.arguments?.getString("message") ?: "Accept?",
                        negativeText = entry.arguments?.getString("negativeText") ?: "CANCEL",
                        positiveText = entry.arguments?.getString("positiveText") ?: "OK",
                        onDismiss = {
                            navController.setNavigationResult(result = false, key = resultKeyOverride)
                            navController.navigateUp()
                        },
                        onNegativeClick = {
                            navController.setNavigationResult(result = false, key = resultKeyOverride)
                            navController.navigateUp()
                        },
                        onPositiveClick = {
                            navController.setNavigationResult(result = true, key = resultKeyOverride)
                            navController.navigateUp()
                        },
                    ) {}
                    BackHandler(enabled = true) {
                        navController.setNavigationResult(result = false, key = resultKeyOverride)
                        navController.navigateUp()
                    }
                }
            }
        }
        companion object {
            const val uiutilDestination: String = "UiUtil"
            private const val resultKeyOverride = "uiutil|binary"

            fun navigateToOverride(navController: NavController, onOverrideClick: () -> Unit) {
                val navBackStackEntry: NavBackStackEntry = navController.currentBackStackEntry!!
                navController.navigate(
                    route = createRoute("$uiutilDestination/binary",
                        optionals = mapOf(
                            "message" to "Already exists. Override?",
                            "positiveText" to "OVERRIDE"
                        )
                    )
                ) {
                    launchSingleTop = true
                }
                getNavigationResult<Boolean>(navBackStackEntry, resultKeyOverride) {
                    if (it) onOverrideClick()
                }
            }

            @Composable
            fun rememberState(navController: NavHostController = rememberNavController()) =
                remember(navController) { UIUtilState(navController) }
        }
    }

    /** Exactly like [LaunchedEffect], differing only in the persistence: This effect launches only once in a composition. */
    @Composable
    fun LaunchedEffectSaveable(key: Any?, func: suspend CoroutineScope.() -> Unit) {
        val alreadyFocussed = rememberSaveable { mutableStateOf(false) }

        if (!alreadyFocussed.value) {
            LaunchedEffect(key) {
                func()
            }
            alreadyFocussed.value = true
        }
    }
    
    /** Creates a route string such as 'somelocation/arg0/arg1?optionalarg2=value' */
    fun createRoute(base: String, args: Collection<String>? = null, optionals: Map<String, String?>? = null): String {
        return base + when (args.isNullOrEmpty()) {
            true -> ""
            false -> "/"+args.joinToString(separator="/")
        } + when(optionals.isNullOrEmpty()) {
            true -> ""
            false -> "?"+optionals.filterValues { v -> v != null }.map { (k, v) -> "$k=$v" }.joinToString(separator = "&")
        }
    }

    open class OpenableMiniState(val open: MutableState<Boolean>) {
        open fun open() {
            open.value = true
        }

        open fun close() {
            open.value = false
        }

        companion object {
            @Composable
            fun rememberState(open: Boolean = false) = remember(open) {
                OpenableMiniState(open = mutableStateOf(open))
            }
        }
    }

    open class StateMiniState(val state: MutableState<LoadingState>, val stateMessage: MutableState<String?>) {
        companion object {
            @Composable
            fun rememberState(state: LoadingState, stateMessage: String? = null) =
                remember(state) { StateMiniState(mutableStateOf(state), mutableStateOf(stateMessage)) }
        }
    }

    fun navigateReplaceStartRoute(navController: NavController, newHomeRoute: String) = with (navController) {
        popBackStack(graph.startDestinationId, true)
        graph.setStartDestination(newHomeRoute)
        navigate(newHomeRoute)
    }

    fun navigateOutOfGraph(navController: NavController) = with (navController) {
        popBackStack(graph.startDestinationId, true)
        navigateUp()
    }

    /**
     * Navigate to a new destination, simultaneously removing the current entry from the backstack.
     * @param navController
     * @param newRoute New route path to follow.
     */
    fun navigatePop(navController: NavController, newRoute: String) {
        with (navController) {
            popBackStack()
            navigate(newRoute)
        }
    }

    fun effect(scope: CoroutineScope, block: suspend () -> Unit) =scope.launch(Dispatchers.IO) { block() }

    fun <T> Flow<T>.stateInViewModel(scope: CoroutineScope, initialValue : T): StateFlow<T> =
        stateIn(scope = scope, started = SharingStarted.Lazily, initialValue = initialValue)

    fun <T> NavController.setNavigationResult(result: T?, key: String = "result") = previousBackStackEntry?.savedStateHandle?.set(key, result)
    fun <T> NavController.navigateForResult(route: String, key: String = "result", onResult: (result: T) -> Unit) {
        val navBackStackEntry = currentBackStackEntry!!
        navigate(route) {
            launchSingleTop = true
        }

        getNavigationResult<T>(navBackStackEntry, key) {
            onResult(it)
        }
    }
    private fun <T> getNavigationResult(navBackStackEntry: NavBackStackEntry, key: String = "result", onResult: (result: T) -> Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && navBackStackEntry.savedStateHandle.contains(key)
            ) {
                val result = navBackStackEntry.savedStateHandle.get<T>(key)
                result?.let(onResult)
                navBackStackEntry.savedStateHandle.remove<T>(key)
            }
        }
        navBackStackEntry.lifecycle.addObserver(observer)

//        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
//            if (event == Lifecycle.Event.ON_DESTROY)
//                navBackStackEntry.lifecycle.removeObserver(observer)
//        })
    }
    fun <T> NavController.forwardNavigationResult(navBackStackEntry: NavBackStackEntry, key: String = "result") {
        getNavigationResult<T>(navBackStackEntry, key) {
            setNavigationResult(result = it, key = key)
        }
    }

    fun NavController.clearNavigationResult(key: String = "result") = previousBackStackEntry?.savedStateHandle?.remove<String>(key)
}