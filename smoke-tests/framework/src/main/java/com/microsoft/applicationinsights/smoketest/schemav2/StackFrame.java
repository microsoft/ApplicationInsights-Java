// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

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
