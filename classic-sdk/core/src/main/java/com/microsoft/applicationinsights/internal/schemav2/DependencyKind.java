// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.internal.schemav2;

@SuppressWarnings("MemberName")
public enum DependencyKind {
  SQL(0),
  Http(1),
  Other(2);

  private final int id;

  public int getValue() {
    return id;
  }

  DependencyKind(int id) {
    this.id = id;
  }
}
