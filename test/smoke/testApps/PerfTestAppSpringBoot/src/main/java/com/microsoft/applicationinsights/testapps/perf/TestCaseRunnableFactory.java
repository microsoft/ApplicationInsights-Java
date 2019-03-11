package com.microsoft.applicationinsights.testapps.perf;

public abstract class TestCaseRunnableFactory {

    private final String name;

    public TestCaseRunnableFactory() {
        this(null);
    }
    public TestCaseRunnableFactory(String name) {
        this.name = name;
    }

    public TestCaseRunnable get() {
        return new TestCaseRunnable(getRunnable(), name);
    }

    protected abstract Runnable getRunnable();
}
