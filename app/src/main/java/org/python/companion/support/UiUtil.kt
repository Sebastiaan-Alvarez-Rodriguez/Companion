package org.python.companion.support

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ArrowLeft
import androidx.compose.material.icons.outlined.ArrowRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
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
import org.python.backend.data.datatype.RenderType
import org.python.companion.R
import org.python.companion.ui.theme.DarkBlue900
import org.python.companion.ui.theme.DarkColorPalette
import java.util.regex.Pattern
import kotlin.math.roundToInt

/** Simple enum representing loading state of asynchronously loading objects. */
enum class LoadingState {
    LOADING, READY, FAILED, OK
}

object UiUtil {
    @Composable
    fun NestedIcon(mainIcon: ImageVector, modifier: Modifier = Modifier, description: String? = null, sideIcon: ImageVector, sideModifier: Modifier = Modifier, sideDescription: String? = null) {
        Box(modifier = modifier) {
            Icon(mainIcon, contentDescription = description)
            Icon(sideIcon, modifier = sideModifier
                .align(Alignment.BottomEnd)
                .background(DarkBlue900, shape = CircleShape), contentDescription = sideDescription)
        }
    }

    @Composable
    fun LabelledCheckBox(checked: Boolean, label: AnnotatedString, onCheckedChange: (Boolean) -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onCheckedChange(checked) }
                )
                .semantics(mergeDescendants = true) {}) {
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
        showableObjectFunc: @Composable () -> Unit = {}
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

    @Composable
    fun <T : Any> GenericList(
        prefix: @Composable (LazyItemScope.() -> Unit)? = null,
        items: Flow<PagingData<T>>,
        isLoading: Boolean,
        showItemFunc: @Composable LazyItemScope.(item: T) -> Unit,
        fab: (@Composable BoxScope.() -> Unit)? = null
    ) {
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
    fun SimpleSearchMatchIteratorHeader(currentItem: Int, numItems: Int, onUpdate: (Int) -> Unit) {
        val defaultPadding = dimensionResource(id = R.dimen.padding_default)
        val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)

        GenericListHeader(
            listOf(
                { Text(modifier = Modifier.padding(defaultPadding), text = "${currentItem+1}/$numItems") },
                {
                    Row(modifier = Modifier.padding(defaultPadding)) {
                        IconButton(onClick = { onUpdate(((currentItem + numItems) - 1) % numItems) }) {
                            Icon(imageVector = Icons.Outlined.ArrowLeft, contentDescription = "Previous result")
                        }
                        Spacer(modifier = Modifier.width(tinyPadding))
                        IconButton(onClick = { onUpdate((currentItem + 1) % numItems) }) {
                            Icon(imageVector = Icons.Outlined.ArrowRight, contentDescription = "Next result")
                        }
                    }
                }
            )
        )
    }

    @Composable
    fun simpleScrollable(positions: List<Int>, modifier: Modifier = Modifier, scrollState: ScrollState, scrollableText: @Composable (Modifier, (TextLayoutResult) -> Unit) -> Unit): (Int) -> Unit {
        val scrollDelta = -80
        val coroutineScope = rememberCoroutineScope()

        var parentOffset by remember { mutableStateOf(0f) }
        var contentTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) } // Layout rendering collector

        val executeScroll: (Int) -> Unit = { spanIndex ->
            coroutineScope.launch {
                val charOffset = positions[spanIndex]
                val renderedLineOffset = contentTextLayoutResult!!.getLineForOffset(charOffset)
                val pointOffset = contentTextLayoutResult!!.getLineBottom(renderedLineOffset)
                val finalOffset = (parentOffset + pointOffset).roundToInt()
                scrollState.animateScrollTo(finalOffset)
            }
        }
        scrollableText(
            modifier.onGloballyPositioned { coordinates -> parentOffset = coordinates.parentLayoutCoordinates!!.positionInParent().y+scrollDelta }
        ) { layout -> contentTextLayoutResult = layout }
        return executeScroll
    }

    @Composable
    fun simpleScrollableRenderText(
        text: AnnotatedString,
        modifier: Modifier = Modifier,
        renderType: RenderType,
        rendererCache: RendererCache? = null,
        itemDrawCache: ItemDrawCache? = null,
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontStyle: FontStyle? = null,
        fontWeight: FontWeight? = null,
        fontFamily: FontFamily? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        textDecoration: TextDecoration? = null,
        textAlign: TextAlign? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        inlineContent: Map<String, InlineTextContent> = mapOf(),
        style: TextStyle = LocalTextStyle.current,
        isTextSelectable: Boolean = false,
        scrollState: ScrollState
    ) = simpleScrollable(
        positions = text.spanStyles.map { it.start },
        modifier = modifier,
        scrollState = scrollState
    ) { outModifier, layoutResultFunc ->
        RenderUtil.RenderText(
            text = text, modifier = outModifier, renderType = renderType, rendererCache = rendererCache,
            itemDrawCache = itemDrawCache, color, fontSize, fontStyle, fontWeight, fontFamily,
            letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines,
            inlineContent, layoutResultFunc, style, isTextSelectable = isTextSelectable
        )
    }

    @Composable
    fun LinkifyText(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontStyle: FontStyle? = null,
        fontWeight: FontWeight? = null,
        fontFamily: FontFamily? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        textDecoration: TextDecoration? = null,
        textAlign: TextAlign? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        inlineContent: Map<String, InlineTextContent> = mapOf(),
        onTextLayout: (TextLayoutResult) -> Unit = {},
        style: TextStyle = LocalTextStyle.current
    ) {
        LinkifyText(AnnotatedString(text), modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, inlineContent, onTextLayout, style)
    }

    /**
     * Just like `Text()` from Compose, but makes links clickable.
     * If no scheme (http, https) is present, prefixes https.
     */
    @Composable
    fun LinkifyText(
        text: AnnotatedString,
        modifier: Modifier = Modifier,
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontStyle: FontStyle? = null,
        fontWeight: FontWeight? = null,
        fontFamily: FontFamily? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        textDecoration: TextDecoration? = null,
        textAlign: TextAlign? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        inlineContent: Map<String, InlineTextContent> = mapOf(),
        onTextLayout: (TextLayoutResult) -> Unit = {},
        style: TextStyle = LocalTextStyle.current
    ) {
        val uriHandler = LocalUriHandler.current
        val layoutResult = remember {
            mutableStateOf<TextLayoutResult?>(null)
        }
        val linksList = extractUrls(text)
        val annotatedString = buildAnnotatedString {
            append(text)
            linksList.forEach {
                addStyle(
                    style = SpanStyle(
                        color = DarkColorPalette.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = it.start,
                    end = it.end
                )
                addStringAnnotation(tag = "URL", annotation = it.url, start = it.start, end = it.end)
            }
        }
        Text(
            text = annotatedString,
            modifier = modifier.pointerInput(Unit) {
                detectTapGestures { offsetPosition ->
                    layoutResult.value?.let {
                        val position = it.getOffsetForPosition(offsetPosition)
                        annotatedString.getStringAnnotations(position, position).firstOrNull()?.let { result -> 
                            if (result.tag == "URL")
                                uriHandler.openUri(result.item)
                        }
                    }
                }
            },
            color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration,
            textAlign, lineHeight, overflow, softWrap, maxLines, inlineContent,
            { layoutResult.value = it; onTextLayout(it) }, style
        )
    }

    private fun extractUrls(text: AnnotatedString): List<LinkInfo> {
        val urlPattern: Pattern = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?)://|www\\.)" + 
                    "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+/?)*" +
                    "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL
        )
        val matcher = urlPattern.matcher(text)
        var matchStart: Int
        var matchEnd: Int
        val links = arrayListOf<LinkInfo>()

        while (matcher.find()) {
            matchStart = matcher.start(1)
            matchEnd = matcher.end()

            var url = text.substring(matchStart, matchEnd)
            if (!url.startsWith("http://") && !url.startsWith("https://"))
                url = "https://$url"

            links.add(LinkInfo(url, matchStart, matchEnd))
        }
        return links
    }

    private data class LinkInfo(
        val url: String,
        val start: Int,
        val end: Int
    )

    @Composable
    fun simpleScrollableText(
        text: AnnotatedString,
        modifier: Modifier = Modifier,
        fontSize: TextUnit = TextUnit.Unspecified,
        scrollState: ScrollState
    ) = simpleScrollable(
        positions = text.spanStyles.map { it.start },
        modifier = modifier,
        scrollState = scrollState
    ) { outModifier, layoutResultFunc ->
        Text(text, modifier = outModifier, fontSize = fontSize, onTextLayout = layoutResultFunc)
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
                    )
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

            fun navigateToDelete(navController: NavController, onDeleteClick: () -> Unit) {
                val navBackStackEntry: NavBackStackEntry = navController.currentBackStackEntry!!
                navController.navigate(
                    route = createRoute("$uiutilDestination/binary",
                        optionals = mapOf(
                            "message" to "Deletion cannot be undone. Are you sure?",
                            "positiveText" to "DELETE"
                        )
                    )
                ) {
                    launchSingleTop = true
                }
                getNavigationResult<Boolean>(navBackStackEntry, resultKeyOverride) {
                    if (it) onDeleteClick()
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
        val alreadyExecuted = rememberSaveable { mutableStateOf(false) }

        if (!alreadyExecuted.value) {
            LaunchedEffect(key) {
                func()
            }
            alreadyExecuted.value = true
        }
    }
    
    /**
     * Creates a route string such as 'somelocation/arg0/arg1?optionalarg2=value'
     * @param base Base path, NOT ending on '/'.
     * @param args Required arguments to chain after the base path.
     * @param optionals Optional named arguments. Arguments with `null` value are filtered out.
     */
    fun createRoute(base: String, args: Collection<String>? = null, optionals: Map<String, String?>? = null): String {
        return base + when (args.isNullOrEmpty()) {
            true -> ""
            false -> "/"+args.joinToString(separator="/")
        } + when(optionals.isNullOrEmpty()) {
            true -> ""
            false -> "?"+optionals.filterValues { v -> v != null }.map { (k, v) -> "$k=$v" }.joinToString(separator = "&")
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