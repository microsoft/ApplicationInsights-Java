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

package com.microsoft.applicationinsights.smoketest.schemav2;

/** Data contract class StackFrame. */
@SuppressWarnings("unused")
public class StackFrame {
  /** Backing field for property Level. */
  private int level;

  /** Backing field for property Method. */
  private String method;

  /** Backing field for property Assembly. */
  private String assembly;

  /** Backing field for property FileName. */
  private String fileName;

  /** Backing field for property Line. */
  private int line;

  /** Initializes a new instance of the StackFrame class. */
  public StackFrame() {}

  /** Sets the Level property. */
  public void setLevel(int value) {
    this.level = value;
  }

  /** Sets the Method property. */
  public void setMethod(String value) {
    this.method = value;
  }

  /** Sets the Assembly property. */
  public void setAssembly(String value) {
    this.assembly = value;
  }

  /** Sets the FileName property. */
  public void setFileName(String value) {
    this.fileName = value;
  }

  /** Sets the Line property. */
  public void setLine(int value) {
    this.line = value;
  }
}
