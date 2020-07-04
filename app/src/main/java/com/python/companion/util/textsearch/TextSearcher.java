package com.python.companion.util.textsearch;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.python.companion.util.genericinterfaces.ErrorListener;
import com.python.companion.util.genericinterfaces.FinishListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TextSearcher {
    protected final String fulltext;
    protected List<Pair<Integer, Integer>> results;
    /** Location in results. 0 indicates user is at first match, 1 at second match, etc. */


    public TextSearcher(@NonNull String fulltext) {
        this.fulltext = fulltext;
        results = new ArrayList<>();
    }

    public void submit(@NonNull String query, @NonNull FinishListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            int idx = fulltext.indexOf(query);
            while (idx != -1) {
                results.add(new Pair<>(idx, idx + query.length()));
                idx = fulltext.indexOf(query, idx + query.length());
            }
            listener.onFinish();
        });
    }

    public void submitRegex(@NonNull String query, @NonNull ErrorListener errorListener, @NonNull FinishListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Pattern pattern = Pattern.compile(query);
                Matcher matcher = pattern.matcher(fulltext);
                results = new ArrayList<>();
                while(matcher.find())
                    results.add(new Pair<>(matcher.start(), matcher.end()-matcher.start()));
                listener.onFinish();
            } catch (PatternSyntaxException e) {
                errorListener.onError(e.getLocalizedMessage());
            }
        });
    }

    /**
     *
     * @param index Index of result to return
     * @return Result specified by index
     * @throws IllegalArgumentException when index is out of bounds. For upper bound, see {@link TextSearcher#resultAmount()}
     */
    public final Pair<Integer, Integer> get(int index) {
        if (index > results.size() || index < 0)
            throw new IllegalArgumentException("We only have "+results.size()+" results, cannot get result "+index);
        return results.get(index);
    }


    /** @return Amount of results found */
    public int resultAmount() {
        return results.size();
    }

    public Iterator<Pair<Integer, Integer>> iterator() {
        return new Iterator<Pair<Integer, Integer>>() {
                private int location = 0;
                @Override
                public boolean hasNext() {
                    return location < results.size();
                }

                @Override
                public final Pair<Integer, Integer> next() {
                    if (!hasNext())
                        throw new NoSuchElementException("End reached");
                    Pair<Integer, Integer> val = results.get(location);
                    ++location;
                    return val;
                }
            };
    }

    public Iterator<Pair<Integer, Integer>> reverse_iterator() {
        return new Iterator<Pair<Integer, Integer>>() {
            protected int location = results.size()-1;

            @Override
            public boolean hasNext() {
                return location >= 0;
            }

            @Override
            public final Pair<Integer, Integer> next() {
                if (!hasNext())
                    throw new NoSuchElementException("End reached");
                Pair<Integer, Integer> val = results.get(location);
                --location;
                return val;
            }
        };
    }

}
