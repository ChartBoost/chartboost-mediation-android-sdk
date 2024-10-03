package com.chartboost.sdk.test;

public class SimulatedException extends RuntimeException {
    public SimulatedException(String msg) {
        super(msg);
    }

    public SimulatedException() {
        this("simulated");
    }
}
