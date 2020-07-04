package com.python.companion.util;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import com.python.companion.ui.notes.note.NoteType;

import java.util.concurrent.Executors;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import ru.noties.jlatexmath.JLatexMathDrawable;

public class RenderUtil {
    private static Markwon.Builder getStandardMDRenderer(Context context) {
        return Markwon.builder(context)
                .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS));
    }

    private static Markwon.Builder getStandardLatexMDRenderer(View view, Context context, float textSize) {
        return getStandardMDRenderer(context)
                .usePlugin(MarkwonInlineParserPlugin.create())
                .usePlugin(JLatexMathPlugin.create(textSize, builder -> {
                    builder.inlinesEnabled(true);
                    builder.errorHandler((latex, error) -> {
                        // Receive error and optionally return drawable to be displayed instead
                        Snackbar.make(view, "Latex error: "+error.getLocalizedMessage(), Snackbar.LENGTH_INDEFINITE).show();
                        return null;
                    });
                    builder.executorService(Executors.newCachedThreadPool());

                    //Theme stuff
                    builder.theme().backgroundProvider(() -> new ColorDrawable(0));
                    builder.theme().blockFitCanvas(true);
                    builder.theme().blockHorizontalAlignment(JLatexMathDrawable.ALIGN_CENTER);
                }));
    }

    private static Markwon getMDRenderer(Context context) {
        return getStandardMDRenderer(context).build();
    }

    private static Markwon getLatexMDRenderer(View view, Context context, float textSize) {
        return getStandardLatexMDRenderer(view, context, textSize).build();
    }

    public static void render(@NonNull TextView view, @NonNull String text, @NoteType.Type int type) {
        if (type == NoteType.TYPE_NORMAL) {
            view.setText(text, TextView.BufferType.SPANNABLE);
            return;
        }

        Markwon renderer;
        switch (type) {
            case NoteType.TYPE_MARKDOWN:
                renderer = getMDRenderer(view.getContext());
                break;
            default: case NoteType.TYPE_MARKDOWN_LATEX:
                renderer = getLatexMDRenderer(view, view.getContext(), view.getTextSize());
        }
        renderer.setMarkdown(view, text);
    }
}
