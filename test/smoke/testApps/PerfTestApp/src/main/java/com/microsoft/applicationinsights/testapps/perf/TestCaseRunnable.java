package com.microsoft.applicationinsights.testapps.perf;

public class TestCaseRunnable implements Runnable {
	private final Runnable op;

	private static final int MAX_LOOP = 100;

	public TestCaseRunnable(Runnable op) {
		if (op == null) {
			this.op = new Runnable(){
				@Override
				public void run() {
				}
			};
		} else {
			this.op = op;
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
		op.run();
		post();
	}

	private void churn(String name) {
		System.out.printf("starting %s loop...%n", name);
		double d = 100.0;
		for(int i = 1; i <= MAX_LOOP; i++) {
			d += d / Math.abs(-(double)i);
		}
		System.out.printf("%s loop finished (%d): %f%n", name, MAX_LOOP, d);
	}

}