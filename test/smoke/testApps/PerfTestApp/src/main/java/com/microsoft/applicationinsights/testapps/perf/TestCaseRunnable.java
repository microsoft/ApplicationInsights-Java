package com.microsoft.applicationinsights.testapps.perf;

import com.google.common.base.Stopwatch;

import java.util.concurrent.TimeUnit;

public class TestCaseRunnable implements Runnable {
	private final Runnable op;

	private static final int MAX_LOOP = 250;

	private final String name;

	public TestCaseRunnable(Runnable op) {
		this(op, null);
	}

	public TestCaseRunnable(Runnable op, String name) {
		if (op == null) {
			this.op = new Runnable(){
				@Override
				public void run() {
				}
			};
			this.name = (name == null) ? "NOP" : name;
		} else {
			this.op = op;
			this.name = (name == null) ? "some operation" : name;
		}
	}

	private void pre() {
		churn("pre");
	}

	private void post() {
		churn("post");
	}

	public void run() {
		pre();
		Stopwatch sw = Stopwatch.createStarted();
		op.run();
		System.out.printf("%s ran in %dms.%n", name, sw.elapsed(TimeUnit.MILLISECONDS));
		post();
		sw.stop();
	}

	private void churn(String name) {
		Stopwatch sw = Stopwatch.createStarted();
		double d = 100.0;
		for(int i = 1; i <= MAX_LOOP; i++) {
			d += d / Math.abs(-(double)i);
		}
		System.out.printf("%s loop finished %d iterations in %dms (d=%f)%n", name, MAX_LOOP, sw.elapsed(TimeUnit.MILLISECONDS), d);
		sw.stop();
	}

}