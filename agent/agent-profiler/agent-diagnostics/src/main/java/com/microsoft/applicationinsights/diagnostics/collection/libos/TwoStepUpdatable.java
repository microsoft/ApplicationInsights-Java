// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos;

public interface TwoStepUpdatable {

  void poll() throws OperatingSystemInteractionException;

  void update() throws OperatingSystemInteractionException;
}
