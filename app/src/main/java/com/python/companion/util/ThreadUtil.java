package com.python.companion.util;

import android.os.Handler;
import android.os.Looper;

public class ThreadUtil {

    public static void runOnUIThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}
