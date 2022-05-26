package org.python.companion.support

import android.content.Context
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.util.Linkify
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.core.spans.StrongEmphasisSpan
import io.noties.markwon.editor.*
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import org.python.backend.data.datatype.RenderType
import org.python.companion.support.UiUtil.LinkifyText
import ru.noties.jlatexmath.JLatexMathDrawable
import java.util.concurrent.Executors


/** Helper utils to render text with special meaning. */
object RenderUtil {

    @Composable
    fun RenderTextField(
        value: String,
        renderType: RenderType,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        readOnly: Boolean = false,
        textStyle: TextStyle = LocalTextStyle.current,
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        isError: Boolean = false,
        visualTransformation: VisualTransformation = VisualTransformation.None,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        keyboardActions: KeyboardActions = KeyboardActions.Default,
        singleLine: Boolean = false,
        maxLines: Int = Int.MAX_VALUE,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        shape: Shape = MaterialTheme.shapes.small,
        colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
    ) {
        when (renderType) {
            RenderType.DEFAULT ->
                OutlinedTextField(
                    value, onValueChange, modifier, enabled, readOnly, textStyle, label,
                    placeholder, leadingIcon, trailingIcon, isError, visualTransformation, keyboardOptions,
                    keyboardActions, singleLine, maxLines, interactionSource, shape, colors
                )
            RenderType.MARKDOWN, RenderType.LATEX ->
                MarkdownEditorTextField(
                    value, renderType, onValueChange, modifier, enabled, readOnly, textStyle, label,
                    placeholder, leadingIcon, trailingIcon, isError, visualTransformation, keyboardOptions,
                    keyboardActions, singleLine, maxLines, interactionSource, shape, colors
                )
        }
    }

    @Composable
    private fun MarkdownEditorTextField(
        value: String,
        renderType: RenderType,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        readOnly: Boolean = false,
        textStyle: TextStyle = LocalTextStyle.current,
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        isError: Boolean = false,
        visualTransformation: VisualTransformation = VisualTransformation.None,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        keyboardActions: KeyboardActions = KeyboardActions.Default,
        singleLine: Boolean = false,
        maxLines: Int = Int.MAX_VALUE,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        shape: Shape = MaterialTheme.shapes.small,
        colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
    ) {
        val textColor = colors.textColor(enabled = enabled).value
        val backgroundColor = colors.backgroundColor(enabled = enabled).value

        val context: Context = LocalContext.current
        val markwonEditor: MarkwonEditor = remember {
            createMarkdownEditor(renderType, context, textSize = textStyle.fontSize, onError = {_, _ -> })
        }
        val backgroundExecutors = remember { Executors.newCachedThreadPool() }

        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                val editText = createEditText(
                    value = value,
                    context = ctx,
                    enabled = enabled,
                    readOnly = readOnly,
                    textStyle = textStyle,
                    isError = isError,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    textColor = textColor,
                    backgroundColor = backgroundColor
                )
                editText.addTextChangedListener(
                    MarkwonEditorTextWatcher.withPreRender(markwonEditor, backgroundExecutors, editText)
                )
                editText.addTextChangedListener(
                    object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            if (s != null)
                                onValueChange(s.toString())
                        }

                        override fun afterTextChanged(s: Editable?) {}
                    }
                )
                return@AndroidView editText
            },
            update = { editText ->
                MarkwonEditorTextWatcher.withPreRender(markwonEditor, backgroundExecutors, editText)
            }
        )
    }

    private fun createEditText(
        value: String,
        context: Context,
//        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        readOnly: Boolean = false,
        textStyle: TextStyle,
//        label: @Composable (() -> Unit)? = null,
//        placeholder: @Composable (() -> Unit)? = null,
//        leadingIcon: @Composable (() -> Unit)? = null,
//        trailingIcon: @Composable (() -> Unit)? = null,
        isError: Boolean = false,
//        visualTransformation: VisualTransformation = VisualTransformation.None,
//        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
//        keyboardActions: KeyboardActions = KeyboardActions.Default,
        singleLine: Boolean = false,
        maxLines: Int = Int.MAX_VALUE,
//        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
//        shape: Shape = MaterialTheme.shapes.small,
        textColor: Color,
        backgroundColor: Color
    ): EditText {
        return EditText(context).apply {
            setText(value)
            isEnabled = enabled
            if (readOnly)
                isEnabled = false
            if (isError)
                error = ""
            if (singleLine)
                setSingleLine()
            setTextColor(textColor.toArgb())
            setBackgroundColor(backgroundColor.toArgb())
            setMaxLines(maxLines)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, textStyle.fontSize.value)
        }
    }

    private fun createMarkdownEditor(renderType: RenderType, context: Context, textSize: TextUnit, onError: (String, String?) -> Unit) = MarkwonEditor
        .builder(createMarkdownRender(renderType, context, textSize, onError))
        .useEditHandler(object : AbstractEditHandler<StrongEmphasisSpan>() {
            override fun configurePersistedSpans(builder: PersistedSpans.Builder) {
                // Here we define which span is _persisted_ in EditText, it is not removed
                //  from EditText between text changes, but instead - reused (by changing
                //  position). Consider it as a cache for spans. We could use `StrongEmphasisSpan`
                //  here also, but I chose Bold to indicate that this span is not the same
                //  as in off-screen rendered markdown
                val haha: PersistedSpans.SpanFactory<FontWeight> = PersistedSpans.SpanFactory<FontWeight> { Bold }
                val jclass: Class<FontWeight> = Bold::class.java as Class<FontWeight>
                builder.persistSpan(jclass, haha)

            }

            override fun handleMarkdownSpan(
                persistedSpans: PersistedSpans,
                editable: Editable,
                input: String,
                span: StrongEmphasisSpan,
                spanStart: Int,
                spanTextLength: Int
            ) {
                val match = MarkwonEditorUtils.findDelimited(input, spanStart, "**", "__")
                if (match != null)
                    editable.setSpan(
                        // we handle StrongEmphasisSpan and represent it with Bold in EditText
                        //  we still could use StrongEmphasisSpan, but it must be accessed
                        //  via persistedSpans
                        persistedSpans[Bold::class.java],
                        match.start(),
                        match.end(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
            }

            override fun markdownSpanType(): Class<StrongEmphasisSpan> = StrongEmphasisSpan::class.java
        })
        .build()

    @Composable
    fun RenderText(
        text: String,
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
        onTextLayout: (TextLayoutResult) -> Unit = {},
        style: TextStyle = LocalTextStyle.current
    ) = RenderText(
        text = AnnotatedString(text), modifier = modifier, renderType = renderType, rendererCache = rendererCache, itemDrawCache = itemDrawCache,
        color, fontSize, fontStyle, fontWeight, fontFamily,
        letterSpacing, textDecoration, textAlign, lineHeight, overflow,
        softWrap, maxLines, inlineContent, onTextLayout, style
    )

    @Composable
    fun RenderText(
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
        onTextLayout: (TextLayoutResult) -> Unit = {},
        style: TextStyle = LocalTextStyle.current
    ) {
        when (renderType) {
            RenderType.DEFAULT ->
                LinkifyText(text = text, modifier = modifier, color, fontSize, fontStyle, fontWeight,
                    fontFamily, letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap,
                    maxLines, inlineContent, onTextLayout, style
                )
            RenderType.MARKDOWN ->
                MarkdownText(
                    text = text, modifier = modifier, renderType = renderType, rendererCache = rendererCache, itemDrawCache = itemDrawCache,
                    color, fontSize, fontFamily, letterSpacing, textDecoration, textAlign, lineHeight,
                    overflow, softWrap, maxLines, inlineContent, onTextLayout, style
                )
            RenderType.LATEX ->
                MarkdownText(
                    text = text, modifier = modifier, renderType = renderType, rendererCache = rendererCache, itemDrawCache = itemDrawCache,
                    color, fontSize, fontFamily, letterSpacing, textDecoration, textAlign, lineHeight,
                    overflow, softWrap, maxLines, inlineContent, onTextLayout, style
                )
        }
    }

    @Composable
    fun MarkdownText(
        text: AnnotatedString,
        modifier: Modifier = Modifier,
        renderType: RenderType,
        rendererCache: RendererCache? = null,
        itemDrawCache: ItemDrawCache? = null,
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
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
        style: TextStyle = LocalTextStyle.current,
        onClick: (() -> Unit)? = null,
        onError: (String, String?) -> Unit = {_,_->},
        // this option will disable all clicks on links, inside the markdown text
        // it also enable the parent view to receive the click event
        disableLinkMovementMethod: Boolean = false
    ) {
        val defaultColor: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
        val context: Context = LocalContext.current
        val textSize = (if (fontSize == TextUnit.Unspecified) LocalTextStyle.current.fontSize else fontSize).times(2)

        val markdownRender: Markwon = remember(renderType, rendererCache) {
            rendererCache?.getOrPut(renderType) {
                rendererCache.create(renderType = renderType, context, textSize, onError)
            } ?: createMarkdownRender(renderType = renderType, context, textSize, onError)
        }

        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                createTextView(
                    context = ctx,
                    color = color,
                    defaultColor = defaultColor,
                    fontSize = fontSize,
                    fontFamily = fontFamily,
                    letterSpacing = letterSpacing,
                    textDecoration = textDecoration,
                    maxLines = maxLines,
                    style = style,
                    textAlign = textAlign,
                    lineHeight = lineHeight,
                    onClick = onClick,
                )
            },
            update = { textView ->
                if (itemDrawCache != null) {
                    val hashCode = text.hashCode()
                    if (itemDrawCache.hash != hashCode) {
                        itemDrawCache.set(hashCode, markdownRender.toMarkdown(text.text))
                    }
                    markdownRender.setParsedMarkdown(textView, itemDrawCache.cached!!)
                } else {
                    markdownRender.setMarkdown(textView, text.text)
                }
            }
        )
    }

    private fun createTextView(
        context: Context,
        color: Color = Color.Unspecified,
        defaultColor: Color,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontFamily: FontFamily? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        textDecoration: TextDecoration? = null,
        textAlign: TextAlign? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        maxLines: Int = Int.MAX_VALUE,
        style: TextStyle,
        onClick: (() -> Unit)? = null
    ): TextView {
        val textColor = color.takeOrElse { style.color.takeOrElse { defaultColor } }
        val mergedStyle = style.merge(
            TextStyle(
                color = textColor,
                fontSize = fontSize,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = textDecoration,
                textAlign = textAlign,
                lineHeight = lineHeight,
            )
        )
        return TextView(context).apply {

            onClick?.let { setOnClickListener { onClick() } }
            setTextColor(textColor.toArgb())
            setMaxLines(maxLines)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, mergedStyle.fontSize.value)
//            setLetterSpacing(mergedStyle.letterSpacing.value) // Keep turned off

            textAlign?.let { align ->
                textAlignment = when (align) {
                    TextAlign.Left, TextAlign.Start -> View.TEXT_ALIGNMENT_TEXT_START
                    TextAlign.Right, TextAlign.End -> View.TEXT_ALIGNMENT_TEXT_END
                    TextAlign.Center -> View.TEXT_ALIGNMENT_CENTER
                    else -> View.TEXT_ALIGNMENT_TEXT_START
                }
            }
            setLineHeight(mergedStyle.lineHeight.value.toInt())
        }
    }

    @Composable
    fun createMarkdownRender(renderType: RenderType, fontSize: TextUnit = TextUnit.Unspecified, onError: (String, String?) -> Unit): Markwon {
        val context: Context = LocalContext.current
        val textSize = (if (fontSize == TextUnit.Unspecified) LocalTextStyle.current.fontSize else fontSize).times(3)
        return createMarkdownRender(renderType, context, textSize, onError)
    }

    /** Creates a markdown renderer */
    fun createMarkdownRender(renderType: RenderType, context: Context, textSize: TextUnit, onError: (String, String?) -> Unit): Markwon {
        val builder = getStandardMDRenderer(context)
        if (renderType == RenderType.LATEX)
            builder
                .usePlugin(MarkwonInlineParserPlugin.create())
                .usePlugin(JLatexMathPlugin.create(textSize.value) { latexConfigBuilder ->
                    val tmp = latexConfigBuilder
                        .inlinesEnabled(true)
                        .errorHandler { latex, error ->
                            onError(latex, error.localizedMessage)
                            return@errorHandler null
                        }
                        .executorService(Executors.newCachedThreadPool())
                    //Theme stuff
                    tmp.theme()
//                        .backgroundProvider { ColorDrawable(0) }
                        .blockFitCanvas(true)
                        .blockHorizontalAlignment(JLatexMathDrawable.ALIGN_CENTER)
                })
        return builder.build()
    }

    private fun getStandardMDRenderer(context: Context): Markwon.Builder {
        return Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS))
    }
}

class RendererCache(_cache: MutableMap<RenderType, Markwon?>? = null) {
    val cache: MutableMap<RenderType, Markwon?> = _cache ?: mutableMapOf()

    init {
        cache[RenderType.DEFAULT] = null
    }

    fun get(renderType: RenderType): Markwon? = cache.get(renderType)

    fun getOrPut(renderType: RenderType, orPut: () -> Markwon?): Markwon? = cache.getOrPut(renderType, orPut)

    @Composable
    fun create(renderType: RenderType, fontSize: TextUnit = TextUnit.Unspecified, onError: (String, String?) -> Unit): Markwon {
        val context: Context = LocalContext.current
        val textSize = (if (fontSize == TextUnit.Unspecified) LocalTextStyle.current.fontSize else fontSize).times(3)
        return RenderUtil.createMarkdownRender(renderType = renderType, context = context, textSize = textSize, onError = onError)
    }

    fun create(renderType: RenderType, context: Context, textSize: TextUnit, onError: (String, String?) -> Unit): Markwon =
        RenderUtil.createMarkdownRender(renderType = renderType, context = context, textSize = textSize, onError = onError)


    fun store(renderType: RenderType, renderer: Markwon?) {
        cache[renderType] = renderer
    }
}

class DrawCache<T>(_cache: MutableMap<T, ItemDrawCache>? = null) {
    val cache: MutableMap<T, ItemDrawCache> = _cache ?: mutableMapOf()

    fun get(key: T): ItemDrawCache? = cache.get(key)

    inline fun getOrPut(key: T, orPut: () -> ItemDrawCache): ItemDrawCache = cache.getOrPut(key, orPut)

    fun getOrDefaultPut(key: T, default: ItemDrawCache): ItemDrawCache = cache.getOrPut(key) { default }
}

class ItemDrawCache {
    var hash: Int = 0
    private set

    var cached: Spanned? = null
    private set

    fun set(hash: Int, spanned: Spanned) {
        this.hash = hash
        this.cached = spanned
    }
}