package com.python.companion.ui.note;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class NoteType {

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_MARKDOWN = 1;
    public static final int TYPE_MARKDOWN_LATEX = 2;
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_NORMAL, TYPE_MARKDOWN, TYPE_MARKDOWN_LATEX})
    public @interface Type {
    }
}


