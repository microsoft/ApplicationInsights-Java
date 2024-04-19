// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics;

import java.util.function.Function;
import javax.annotation.Nullable;

public interface DiagnosticsValueFinder {

  String getName();

  String getValue(@Nullable Function<String, String> envVarsFunction);
}
