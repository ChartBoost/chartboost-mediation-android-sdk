package com.chartboost.sdk.test;

import com.chartboost.sdk.legacy.CBUnitTestHelper;

import java.util.Stack;

public class PassthroughExceptionsGuard implements AutoCloseable {
    private final Stack<Class> previousExpectedExceptions;

    public PassthroughExceptionsGuard() {
        previousExpectedExceptions = CBUnitTestHelper.swapExpectedExceptions(null);
    }

    @Override
    public void close() {
        CBUnitTestHelper.swapExpectedExceptions(previousExpectedExceptions);
    }
}
