package com.chartboost.sdk.test;

public class TestDisplayMetrics {
    public static TestDisplayMetrics portrait() {
        return new TestDisplayMetrics()
                .withWidth(768)
                .withHeight(1024);
    }

    public static TestDisplayMetrics landscape() {
        return new TestDisplayMetrics()
                .withWidth(1024)
                .withHeight(768);
    }

    private int width = 0;
    private int height = 0;
    private float density = 1.0f;
    private int densityDpi = 47;

    public int height() {
        return height;
    }

    public int width() {
        return width;
    }

    public float density() {
        return density;
    }

    public int densityDpi() {
        return densityDpi;
    }

    public TestDisplayMetrics withHeight(int height) {
        this.height = height;
        return this;
    }

    public TestDisplayMetrics withWidth(int width) {
        this.width = width;
        return this;
    }

    public TestDisplayMetrics withDensity(float density) {
        this.density = density;
        return this;
    }

    public TestDisplayMetrics withDensityDpi(int densityDpi) {
        this.densityDpi = densityDpi;
        return this;
    }
}
