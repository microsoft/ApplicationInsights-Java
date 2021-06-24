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

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class BinaryCalculation {
  private static DecimalFormat getDefaultFormat() {
    DecimalFormat fmt = new DecimalFormat();
    fmt.setRoundingMode(RoundingMode.HALF_EVEN);
    fmt.setMaximumFractionDigits(256); // some arbitrarily large number
    fmt.setNegativePrefix("(-");
    fmt.setNegativeSuffix(")");
    fmt.setGroupingUsed(false);
    return fmt;
  }

  private final double leftOperand;
  private final double rightOperand;
  private final BinaryOperator operator;
  private final DecimalFormat format;

  public BinaryCalculation(
      double leftOperand, double rightOperand, BinaryOperator operator, DecimalFormat format) {
    this.leftOperand = leftOperand;
    this.rightOperand = rightOperand;
    this.operator = operator;
    this.format = format;
  }

  public BinaryCalculation(double leftOperand, double rightOperand, BinaryOperator operator) {
    this(leftOperand, rightOperand, operator, getDefaultFormat());
  }

  public DecimalFormat getFormat() {
    return this.format;
  }

  public double getLeftOperand() {
    return leftOperand;
  }

  public String getLeftOperandFormatted() {
    return this.format.format(getLeftOperand());
  }

  public double getRightOperand() {
    return rightOperand;
  }

  public String getRightOperandFormatted() {
    return this.format.format(getRightOperand());
  }

  public BinaryOperator getOperator() {
    return operator;
  }

  public Object getOperatorSymbol() {
    return getOperator().getSymbol();
  }

  public double result() {
    return getOperator().compute(getLeftOperand(), getRightOperand());
  }

  public String resultFormatted() {
    return getFormat().format(result());
  }
}
