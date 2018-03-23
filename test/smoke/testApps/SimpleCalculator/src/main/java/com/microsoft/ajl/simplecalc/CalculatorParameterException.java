package com.microsoft.ajl.simplecalc;

public class CalculatorParameterException extends Exception {
	private static final long serialVersionUID = -8200839910936319857L;

	public CalculatorParameterException() {
		super();
	}

	public CalculatorParameterException(String message, Throwable cause) {
		super(message, cause);
	}

	public CalculatorParameterException(String message) {
		super(message);
	}

	public CalculatorParameterException(Throwable cause) {
		super(cause);
	}
}