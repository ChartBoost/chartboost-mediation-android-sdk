package com.chartboost.sdk.test;

import java.util.concurrent.locks.Lock;

public class LockGuard implements AutoCloseable {

    private final Lock lock;

    public LockGuard(Lock lock) {
        this.lock = lock;
        lock.lock();
    }

    @Override
    public void close() {
        lock.unlock();
    }
}
