// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.process;

import java.util.Map;

public interface ProcessInfo extends Comparable<ProcessInfo> {

  String getName();

  int getPid();

  String getUid();

  Map<String, String> getMetaData();
}
