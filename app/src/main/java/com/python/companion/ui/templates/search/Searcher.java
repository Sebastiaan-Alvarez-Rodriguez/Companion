package com.python.companion.ui.templates.search;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public abstract class Searcher<T> implements SearchView.OnQueryTextListener {
    protected List<T> backup = null;

    protected EventListener<T> listener;

    public Searcher(EventListener<T> listener) {
        this.listener = listener;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    /**
     * Receives filter changes and computes new results.
     * All responses are handled via {@link EventListener}
     * @param query The string to filter on
     * @return always true (to let SearchView know we handled the searching)
     */
    @Override
    public boolean onQueryTextChange(String query) {
        if (backup == null) //Beginning of search
           backup = listener.onBeginSearch();

        final List<T> filteredList;
        if (query == null || query.isEmpty()) { //End of search
            listener.onFinishSearch(backup);
            backup = null;
        } else {
            filteredList = filter(backup, query.toLowerCase());
            listener.onReceiveFilteredContent(filteredList);
        }
        return true;
    }

    @NonNull
    abstract protected List<T> filter(List<T> list, @NonNull String query);

    public interface EventListener<T> {
        /**
         * Called on the beginning of a search (user starts typing).
         * Important: Make sure the user cannot add any new items now!
         * @return the current list to be searched
         */
        @NonNull List<T> onBeginSearch();

        /**
         * Called when searching is finished (no more text in searchView).
         * Provides the original list (from before the search)
         * @param initial original list
         */
        void onFinishSearch(List<T> initial);

        /**
         * Provides the filtered list, containing only items matching the query thus far
         * @param filtered the filtered list
         */
        void onReceiveFilteredContent(List<T> filtered);
    }

}
