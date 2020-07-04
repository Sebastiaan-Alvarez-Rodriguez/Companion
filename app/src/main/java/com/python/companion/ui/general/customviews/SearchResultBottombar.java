package com.python.companion.ui.general.customviews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;

public class SearchResultBottombar extends BaseTransientBottomBar<SearchResultBottombar> {
    public interface UserDismissListener {
        void onDismiss();
    }

    public static class Builder {
        protected View.OnClickListener onDownListener = null, onUpListener = null;
        protected UserDismissListener dismissListener = null;

        public Builder setOnDownListener(@NonNull View.OnClickListener listener) {
            this.onDownListener = listener;
            return this;
        }

        public Builder setOnUpListener(@NonNull View.OnClickListener listener) {
            this.onUpListener = listener;
            return this;
        }

        public Builder setOnUserDismissListener(@NonNull UserDismissListener dismissListener) {
            this.dismissListener = dismissListener;
            return this;
        }

        public SearchResultBottombar make(@NonNull ViewGroup parent, @Snackbar.Duration int duration) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View content = inflater.inflate(R.layout.bottombar_searchresults, parent, false);

            ContentViewCallback callback= new ContentViewCallback(content);
            SearchResultBottombar customSnackbar = new SearchResultBottombar(parent, content, callback, onDownListener, onUpListener, dismissListener);
            // Remove black background padding on left and right
            customSnackbar.getView().setPadding(0, 0, 0, 0);
            // set snackbar duration
            customSnackbar.setDuration(duration);
            return customSnackbar;
        }
    }

    protected TextView mainText, countText;

    protected @Nullable UserDismissListener dismissListener;

    protected SearchResultBottombar(@NonNull ViewGroup parent, @NonNull View content, @NonNull com.google.android.material.snackbar.ContentViewCallback contentViewCallback, @Nullable View.OnClickListener onDownListener, @Nullable View.OnClickListener onUpListener, @Nullable UserDismissListener dismissListener) {
        super(parent, content, contentViewCallback);
        this.dismissListener = dismissListener;

        content.findViewById(R.id.bottombar_searchresults_down).setOnClickListener(onDownListener);
        content.findViewById(R.id.bottombar_searchresults_up).setOnClickListener(onUpListener);

        mainText = content.findViewById(R.id.bottombar_searchresults_text);
        countText = content.findViewById(R.id.bottombar_searchresults_count);
    }

    public void setMainText(@NonNull String text) {
        mainText.setText(text);
    }

    public void setCountText(@NonNull String text) {
        countText.setText(text);
    }

    public void setCountText(int current, int total) {
        countText.setText(current +"/"+ total);
    }

    @Override
    protected void dispatchDismiss(int event) {
        if (event != BaseCallback.DISMISS_EVENT_MANUAL && dismissListener != null)
            dismissListener.onDismiss();
        super.dispatchDismiss(event);
    }


    protected static class ContentViewCallback implements com.google.android.material.snackbar.ContentViewCallback {
        // view inflated from custom layout
        protected View content;

        protected ContentViewCallback(View content) {
            this.content = content;
        }

        @Override
        public void animateContentIn(int delay, int duration) {
            content.setAlpha(0f);
            ViewCompat.animate(content).alpha(1f).setDuration(duration).setStartDelay(delay);
        }

        @Override
        public void animateContentOut(int delay, int duration) {
            content.setAlpha(1f);
            ViewCompat.animate(content).alpha(0f).setDuration(duration).setStartDelay(delay);
        }
    }
}
