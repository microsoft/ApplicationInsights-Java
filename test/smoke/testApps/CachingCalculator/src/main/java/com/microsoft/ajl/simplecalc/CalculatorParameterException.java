package com.microsoft.ajl.simplecalc;

public class CalculatorParameterException extends Exception {

    public CalculatorParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public CalculatorParameterException(String message) {
        super(message);
    }
}