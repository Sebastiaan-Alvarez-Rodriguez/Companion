package com.python.companion.util;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.util.Linkify;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.python.companion.ui.notes.note.NoteType;

import java.util.concurrent.Executors;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import ru.noties.jlatexmath.JLatexMathDrawable;

public class RenderUtil {
    private static Markwon.Builder getStandardMDRenderer(Context context) {
        return Markwon.builder(context)
                .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS))
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureConfiguration(@NonNull MarkwonConfiguration.Builder builder) {
                        builder.urlProcessor(destination -> !destination.startsWith("http://") && !destination.startsWith("https://") ? "http://"+destination : destination);
                    }
                });
    }

    private static Markwon.Builder getStandardLatexMDRenderer(Context context, float textSize) {
        return getStandardMDRenderer(context)
                .usePlugin(JLatexMathPlugin.create(textSize, builder -> builder
                        .align(JLatexMathDrawable.ALIGN_CENTER)
                        .fitCanvas(true)
                        .backgroundProvider(() -> new ColorDrawable(0))
                .executorService(Executors.newCachedThreadPool())));
    }

    private static Markwon getMDRenderer(Context context) {
        return getStandardMDRenderer(context).build();
    }

    private static Markwon getLatexMDRenderer(Context context, float textSize) {
        return getStandardLatexMDRenderer(context, textSize).build();
    }

    public static void render(@NonNull TextView view, @NonNull String text, @NoteType.Type int type) {
        if (type == NoteType.TYPE_NORMAL) {
            view.setText(text);
            return;
        }

        Markwon renderer;
        switch (type) {
            case NoteType.TYPE_MARKDOWN:
                renderer = getMDRenderer(view.getContext());
                break;
            default: case NoteType.TYPE_MARKDOWN_LATEX:
                renderer = getLatexMDRenderer(view.getContext(), 50);
        }
        renderer.setMarkdown(view, text);
    }
}
