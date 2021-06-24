/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketestapp.model;

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
        throw new UnsupportedOperationException(
            this.toString() + " compute is not yet implemented");
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
