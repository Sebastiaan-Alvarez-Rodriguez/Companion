package com.python.companion.util.threadpool;

import android.util.Log;

import androidx.annotation.Nullable;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ProbableThreadPoolExecutor extends ThreadPoolExecutor {
    private @Nullable ThreadExceptionListener exceptionListener;

    public ProbableThreadPoolExecutor(@Nullable ThreadExceptionListener exceptionListener) {
        this(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), exceptionListener);
    }

    public ProbableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, @Nullable ThreadExceptionListener exceptionListener) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.exceptionListener = exceptionListener;
    }

    public ProbableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, @Nullable ThreadExceptionListener exceptionListener) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.exceptionListener = exceptionListener;
    }

    public ProbableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler, @Nullable ThreadExceptionListener exceptionListener) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        this.exceptionListener = exceptionListener;
    }

    public ProbableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler, @Nullable ThreadExceptionListener exceptionListener) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.exceptionListener = exceptionListener;
    }

    @Override
    public void execute(Runnable command) {
        try {
            super.execute(command);
        } catch (Exception e) {
            if (exceptionListener != null)
                exceptionListener.onException(e);
            Log.i("Probable", "Got a threadpool exception");
        }
    }


}
