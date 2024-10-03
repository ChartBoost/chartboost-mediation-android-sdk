package com.chartboost.sdk.legacy;

// TODO This class seems weird, I don't see why it's awkward or impossible to inject dependencies.
/*
    The purpose of this Factory class is to make it easier to create mock objects in unit tests
    where it would otherwise be awkward or impossible to inject dependencies directly
    by passing mocks down the call stack.
 */
public class Factory {
    private static Factory instance = new Factory();

    public static Factory instance() {
        return instance;
    }

    /*
        install() is only meant to be used by unit tests, to install a TestFactory.
        It's optimized out in the release.
     */
    public static void install(Factory factory) {
        instance = factory;
    }

    /*
        TestFactory overrides this to replace a real instance with a mock, or to spy on a real instance.
     */
    public <T> T intercept(T object) {
        return object;
    }
}
