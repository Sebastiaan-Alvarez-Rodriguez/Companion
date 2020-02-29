package com.python.companion.util.threadpool;

import androidx.annotation.NonNull;

public interface ThreadExceptionListener {
    void onException(@NonNull Exception exception);
}
