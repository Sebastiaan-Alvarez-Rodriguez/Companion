package com.python.companion.ui.general.textviewsearch;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.util.Pair;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;
import com.python.companion.ui.general.customviews.SearchResultBottombar;
import com.python.companion.ui.general.dialog.ErrorDialog;
import com.python.companion.ui.general.spans.ColoredStyleSpan;
import com.python.companion.util.Keyboard;
import com.python.companion.util.ThreadUtil;
import com.python.companion.util.genericinterfaces.FinishListener;
import com.python.companion.util.textsearch.TextSearcher;

import java.util.Iterator;

import static android.graphics.Typeface.BOLD;

public class UITextSearcher {
    protected FragmentManager manager;
    protected ViewGroup layout;
    protected NestedScrollView scrollView;
    protected TextView textView;
    protected SearchView searchView;

    protected @Nullable SearchResultBottombar resultBottombar;

    protected @Nullable TextSearcher searcher;

    protected Spannable original_text;

    protected @Nullable ColoredStyleSpan selected;
    protected int currentResult = 0;

    public UITextSearcher(FragmentManager manager, ViewGroup layout, NestedScrollView scrollView, TextView textView, SearchView searchView, Spannable spannable) {
        this.manager = manager;
        this.layout = layout;
        this.scrollView = scrollView;
        this.textView = textView;
        this.searchView = searchView;

        this.original_text = spannable;
        this.searcher = new TextSearcher(spannable.toString());

        this.resultBottombar = null;
        this.selected = null;
//        textView.setEnabled(true);
//        textView.setFocusable(true);
//        textView.setLongClickable(true);
//        textView.setTextIsSelectable(true);
    }

    @SuppressWarnings("ConstantConditions")
    public void submit(@NonNull String query, boolean isRegex) {
        FinishListener listener = () -> {
            this.selected = null;
            final SpannableString s = markResults(searcher);
            ThreadUtil.runOnUIThread(() -> {
                textView.setText(s, TextView.BufferType.SPANNABLE);
            });

            resultBottombar = new SearchResultBottombar.Builder()
                    .setOnDownListener(v -> {
                        if (searcher.resultAmount() == 0)
                            return;
                        final int prev = currentResult;
                        Pair<Integer, Integer> x = next();
                        if (currentResult != prev) {
                            scrollToResult(x.first);
                            setSelection(x.first, x.second);
                        }
                    })
                    .setOnUpListener(v -> {
                        if (searcher.resultAmount() == 0)
                            return;
                        final int prev = currentResult;
                        Pair<Integer, Integer> x = prev();
                        if (currentResult != prev) {
                            scrollToResult(x.first);
                            setSelection(x.first, x.second);
                        }
                    })
                    .setOnUserDismissListener(UITextSearcher.this::finish)
                    .make(layout, Snackbar.LENGTH_INDEFINITE);
            resultBottombar.setMainText("Found "+searcher.resultAmount()+" results");
            resultBottombar.show();

            if (searcher.resultAmount() > 0) {
                selected = new ColoredStyleSpan(BOLD, Color.BLUE);
                Pair<Integer, Integer> firstResult = searcher.get(0);
                scrollToResult(firstResult.first);
                setSelection(firstResult.first, firstResult.second);
            } else {
                resultBottombar.setCountText(0, 0);
            }
        };

        if (isRegex)
            searcher.submitRegex(query, error -> {
                ErrorDialog dialog = new ErrorDialog.Builder()
                        .setTitle("Regex Error")
                        .setProblem(error)
                        .build();
                dialog.show(manager, null);
            }, listener);
        else
            searcher.submit(query, listener);
        Keyboard.hideKeyboard(textView.getContext(), textView);
    }

    /** Call to stop the search process and to revert text to its original, non-spanned variant */
    public void finish() {
        textView.setText(original_text, TextView.BufferType.SPANNABLE);
        if (resultBottombar != null)
            resultBottombar.dismiss();
    }

    /** Scrolls scrollview in UI to offset location. Also updates bottombar to reflect this*/
    protected void scrollToResult(int offset) {
        int lineNumber = textView.getLayout().getLineForOffset(offset);
        scrollView.scrollTo(0, textView.getLayout().getLineTop(lineNumber));
        if (resultBottombar != null)
            resultBottombar.setCountText(currentResult+1, searcher.resultAmount());
    }

    /** Selects specified offsets in textview */
    protected void setSelection(int start, int offset) {
        ThreadUtil.runOnUIThread(() -> {
            Spannable s = ((Spannable) textView.getText());
            s.setSpan(selected, start, start+offset, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        });
    }

    /**
     * Marks all results in specified color
     * @param searcher Object containing search results
     * @return String with spans marking the results
     */
    protected final SpannableString markResults(TextSearcher searcher) {
        SpannableString spannable = new SpannableString(original_text);
        Iterator<Pair<Integer,Integer>> it = searcher.iterator();
        while (it.hasNext()) {
            Pair<Integer, Integer> val = it.next();
            ColoredStyleSpan span = new ColoredStyleSpan(BOLD, Color.RED);
            spannable.setSpan(span, val.first, val.first+val.second, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    protected final Pair<Integer, Integer> next() {
        if (searcher.resultAmount() == 0)
            throw new IllegalStateException("No results to iterate over");
        if (currentResult == searcher.resultAmount()-1) // We are at last match, wrap around
            currentResult = 0;
        else
            currentResult += 1;
        return searcher.get(currentResult);
    }

    protected final Pair<Integer, Integer> prev() {
        if (searcher.resultAmount() == 0)
            throw new IllegalStateException("No results to iterate over");
        if (currentResult == 0) // We are at top match, wrap around
            currentResult = searcher.resultAmount()-1;
        else
            currentResult -= 1;
        return searcher.get(currentResult);
    }
}
