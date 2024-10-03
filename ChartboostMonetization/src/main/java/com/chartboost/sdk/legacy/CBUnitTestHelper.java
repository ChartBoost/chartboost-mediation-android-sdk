package com.chartboost.sdk.legacy;

import java.util.Stack;

public class CBUnitTestHelper {
    private static Stack<Class> expectedExceptions = null;

    public static synchronized Stack<Class> swapExpectedExceptions(Stack<Class> newExpectedExceptions) {
        Stack<Class> previousExpectedExceptions = expectedExceptions;
        expectedExceptions = newExpectedExceptions;
        return previousExpectedExceptions;
    }
}
