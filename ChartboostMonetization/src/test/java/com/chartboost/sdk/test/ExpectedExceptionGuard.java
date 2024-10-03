package com.chartboost.sdk.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.chartboost.sdk.legacy.CBUnitTestHelper;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

/*
    Wrap test code with an ExpectedExceptionGuard when it is known to throw exception
    that is caught, reported to CBTrack, but otherwise hidden.

    This will stop CBTrack.trackException from printing the stack trace to the console,
    keeping the console log from getting overwhelmed with expected exceptions.
        (A number of tests are built specifically to simulate exceptions being thrown.
        This is meant for these.)

    The guard will cause the test to fail if either:
     - a different exception type is thrown.
     - the expected exception type is not thrown.

    try(ExpectedExceptionGuard guard = new ExpectedExceptionGuard(ExceptionType.class) { ... }

    This guard can nest but cannot be used from separate threads.
 */
public class ExpectedExceptionGuard implements AutoCloseable {
    // This allows nesting, but more importantly if one test fails by not reporting an exception,
    // the cleanup in close() makes it so every following test won't then fail spuriously.
    private final Stack<Class> previousExpectedExceptions;

    public ExpectedExceptionGuard() {
        this(new Stack<Class>());
    }

    public ExpectedExceptionGuard(Class expectedClass) {
        this(objectStack(Collections.singletonList(expectedClass)));
    }

    public ExpectedExceptionGuard(List<Class> expectedClasses) {
        this(objectStack(expectedClasses));
    }

    private static Stack<Class> objectStack(List<Class> expectedClasses) {
        Stack<Class> expectedExceptions = new Stack<>();
        for (Class expectedClass : expectedClasses)
            expectedExceptions.push(expectedClass);
        return expectedExceptions;
    }


    private ExpectedExceptionGuard(Stack<Class> expectedExceptions) {
        previousExpectedExceptions = CBUnitTestHelper.swapExpectedExceptions(expectedExceptions);
    }

    @Override
    public void close() {
        Stack<Class> remainingExceptions = CBUnitTestHelper.swapExpectedExceptions(previousExpectedExceptions);
        if (com.chartboost.sdk.BuildConfig.DEBUG) { // note sdk.BuildConfig not sdk.test.BuildConfig
            // in release BuildConfig, CBTrack.trackException doesn't check here.
            assertThat(remainingExceptions, is(equalTo(new Stack<Class>())));
        }
    }
}
