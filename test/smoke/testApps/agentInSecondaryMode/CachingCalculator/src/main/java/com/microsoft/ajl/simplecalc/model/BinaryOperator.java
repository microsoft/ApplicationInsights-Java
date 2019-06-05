package com.microsoft.ajl.simplecalc.model;

public enum BinaryOperator {
	ADDITION("plus", "+"),
	SUBTRACTION("minus", "\u2212");
	
	private final String verb;
	private final String symbol;

	BinaryOperator(String verb, String symbol) {
		this.verb = verb;
		this.symbol = symbol;
	}

	public String getVerb() {
		return verb;
	}

	public String getSymbol() {
		return symbol;
	}
	
	public double compute(double leftOperand, double rightOperand) {
		switch (this) {
			case ADDITION:
				return leftOperand + rightOperand;
			case SUBTRACTION:
				return leftOperand - rightOperand;
			default:
				throw new UnsupportedOperationException(this.toString() + " compute is not yet implemented");
		}
	}
	
	public static BinaryOperator fromVerb(String verb) {
		if (verb == null || verb.length() == 0) {
			throw new IllegalArgumentException("verb must be non-null, non-empty");
		}
		for (BinaryOperator op : BinaryOperator.values()) {
			if (op.getVerb().equals(verb)) {
				return op;
			}
		}
		return null;
	}
}
