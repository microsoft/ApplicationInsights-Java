package com.microsoft.applicationinsights.testapps.perf;

public abstract class TestCaseRunnableFactory {
    public TestCaseRunnable get() {
        return new TestCaseRunnable(getRunnable());
    }
    protected abstract Runnable getRunnable();
}
