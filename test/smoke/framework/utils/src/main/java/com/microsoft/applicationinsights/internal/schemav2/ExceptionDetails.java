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

package com.microsoft.applicationinsights.internal.schemav2;

import java.util.ArrayList;
import java.util.List;

/** Data contract class ExceptionDetails. */
@SuppressWarnings("unused")
public class ExceptionDetails {
  /** Backing field for property Id. */
  private int id;

  /** Backing field for property OuterId. */
  private int outerId;

  /** Backing field for property TypeName. */
  private String typeName;

  /** Backing field for property Message. */
  private String message;

  /** Backing field for property HasFullStack. */
  private boolean hasFullStack = true;

  /** Backing field for property Stack. */
  private String stack;

  /** Backing field for property ParsedStack. */
  private List<StackFrame> parsedStack;

  /** Initializes a new instance of the ExceptionDetails class. */
  public ExceptionDetails() {}

  /** Gets the Id property. */
  public int getId() {
    return this.id;
  }

  /** Sets the Id property. */
  public void setId(int value) {
    this.id = value;
  }

  /** Sets the OuterId property. */
  public void setOuterId(int value) {
    this.outerId = value;
  }

  /** Gets the TypeName property. */
  public String getTypeName() {
    return this.typeName;
  }

  /** Sets the TypeName property. */
  public void setTypeName(String value) {
    this.typeName = value;
  }

  /** Gets the Message property. */
  public String getMessage() {
    return this.message;
  }

  /** Sets the Message property. */
  public void setMessage(String value) {
    this.message = value;
  }

  /** Sets the HasFullStack property. */
  public void setHasFullStack(boolean value) {
    this.hasFullStack = value;
  }

  /** Sets the Stack property. */
  public void setStack(String value) {
    this.stack = value;
  }

  /** Gets the ParsedStack property. */
  public List<StackFrame> getParsedStack() {
    if (this.parsedStack == null) {
      this.parsedStack = new ArrayList<>();
    }
    return this.parsedStack;
  }

  /** Sets the ParsedStack property. */
  public void setParsedStack(List<StackFrame> value) {
    this.parsedStack = value;
  }
}
