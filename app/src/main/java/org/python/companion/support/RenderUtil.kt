package org.python.companion.support

import android.content.Context
import android.text.util.Linkify
import com.google.android.material.snackbar.Snackbar
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import org.python.backend.data.datatype.RenderType

object RenderUtil {
    fun render(view: TextView, text: String, type: RenderType) {
        if (type == NoteType.TYPE_NORMAL) {
            view.setText(text, TextView.BufferType.SPANNABLE)
            return
        }
        val renderer: Markwon = when (type) {
            RenderType.MARKDOWN -> getMDRenderer(view.getContext())
            RenderType.LATEX -> getLatexMDRenderer(
                view,
                view.getContext(),
                view.getTextSize()
            )
            else -> getLatexMDRenderer(view, view.getContext(), view.getTextSize())
        }
        renderer.setMarkdown(view, text)
    }

    private fun getStandardMDRenderer(context: Context): Markwon.Builder {
        return Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS))
    }

    private fun getStandardLatexMDRenderer(
        view: View,
        context: Context,
        textSize: Float
    ): Markwon.Builder {
        return getStandardMDRenderer(context)
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(JLatexMathPlugin.create(textSize) { builder ->
                builder.inlinesEnabled(true)
                builder.errorHandler { latex, error ->
                    // Receive error and optionally return drawable to be displayed instead
                    Snackbar.make(
                        view,
                        "Latex error: " + error.getLocalizedMessage(),
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                    null
                }
                builder.executorService(Executors.newCachedThreadPool())

                //Theme stuff
                builder.theme().backgroundProvider { ColorDrawable(0) }
                builder.theme().blockFitCanvas(true)
                builder.theme().blockHorizontalAlignment(JLatexMathDrawable.ALIGN_CENTER)
            })
    }

    private fun getMDRenderer(context: Context): Markwon {
        return getStandardMDRenderer(context).build()
    }

    private fun getLatexMDRenderer(view: View, context: Context, textSize: Float): Markwon {
        return getStandardLatexMDRenderer(view, context, textSize).build()
    }
}