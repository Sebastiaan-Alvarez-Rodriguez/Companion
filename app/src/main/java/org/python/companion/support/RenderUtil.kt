package org.python.companion.support

import android.content.Context
import android.text.util.Linkify
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import org.python.backend.data.datatype.RenderType
import ru.noties.jlatexmath.JLatexMathDrawable

object RenderUtil {

    @Composable
    fun RenderText(
        text: String,
        renderType: RenderType,
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
    ) = RenderText(
        text = AnnotatedString(text), renderType = renderType,
        modifier, color, fontSize, fontStyle, fontWeight, fontFamily,
        letterSpacing, textDecoration, textAlign, lineHeight, overflow,
        softWrap, maxLines, inlineContent, onTextLayout, style
    )

    @Composable
    fun RenderText(
        text: AnnotatedString,
        renderType: RenderType,
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
        when (renderType) {
            RenderType.DEFAULT ->
                Text(text = text, modifier = modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, inlineContent, onTextLayout, style)
            RenderType.MARKDOWN ->
                MarkdownText(text = text, modifier = modifier, useLatex = false, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, inlineContent, onTextLayout, style)
            RenderType.LATEX ->
                MarkdownText(text = text, modifier = modifier, useLatex = true, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, inlineContent, onTextLayout, style)

        }
    }
    @Composable
    fun MarkdownText(
        text: AnnotatedString,
        modifier: Modifier = Modifier,
        useLatex: Boolean = false,
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
        style: TextStyle = LocalTextStyle.current,
        onClick: (() -> Unit)? = null,
        onError: (String, String?) -> Unit = {_,_->},
        // this option will disable all clicks on links, inside the markdown text
        // it also enable the parent view to receive the click event
        disableLinkMovementMethod: Boolean = false,
    ) {
        val defaultColor: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
        val context: Context = LocalContext.current
        val textSize = if (fontSize == TextUnit.Unspecified) LocalTextStyle.current.fontSize else fontSize

        val markdownRender: Markwon = rememberSaveable(useLatex) {
            createMarkdownRender(context, textSize = textSize, withLatex = useLatex, onError = onError)
        }
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                createTextView(
                    context = ctx,
                    color = color,
                    defaultColor = defaultColor,
                    fontSize = fontSize,
                    maxLines = maxLines,
                    style = style,
                    textAlign = textAlign,
                    onClick = onClick,
                )
            },
            update = { textView ->
                markdownRender.setMarkdown(textView, text.text)
                if (disableLinkMovementMethod) {
                    textView.movementMethod = null
                }
            }
        )
    }

    private fun createTextView(
        context: Context,
        color: Color = Color.Unspecified,
        defaultColor: Color,
        fontSize: TextUnit = TextUnit.Unspecified,
        textAlign: TextAlign? = null,
        maxLines: Int = Int.MAX_VALUE,
        style: TextStyle,
        onClick: (() -> Unit)? = null
    ): TextView {
        val textColor = color.takeOrElse { style.color.takeOrElse { defaultColor } }
        val mergedStyle = style.merge(
            TextStyle(
                color = textColor,
                fontSize = fontSize,
                textAlign = textAlign,
            )
        )
        return TextView(context).apply {
            onClick?.let { setOnClickListener { onClick() } }
            setTextColor(textColor.toArgb())
            setMaxLines(maxLines)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, mergedStyle.fontSize.value)

            textAlign?.let { align ->
                textAlignment = when (align) {
                    TextAlign.Left, TextAlign.Start -> View.TEXT_ALIGNMENT_TEXT_START
                    TextAlign.Right, TextAlign.End -> View.TEXT_ALIGNMENT_TEXT_END
                    TextAlign.Center -> View.TEXT_ALIGNMENT_CENTER
                    else -> View.TEXT_ALIGNMENT_TEXT_START
                }
            }
        }
    }

    private fun createMarkdownRender(context: Context, textSize: TextUnit, withLatex: Boolean = false, onError: (String, String?) -> Unit): Markwon {
        val builder = getStandardMDRenderer(context)
        if (withLatex)
            builder
                .usePlugin(MarkwonInlineParserPlugin.create())
                .usePlugin(JLatexMathPlugin.create(textSize.value) { latexConfigBuilder ->
                    val tmp = latexConfigBuilder
                        .inlinesEnabled(true)
                        .errorHandler { latex, error ->
                            onError(latex, error.localizedMessage)
                            return@errorHandler null
                        }
//                        .executorService(Executors.newCachedThreadPool()) TODO: Can be removed?
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