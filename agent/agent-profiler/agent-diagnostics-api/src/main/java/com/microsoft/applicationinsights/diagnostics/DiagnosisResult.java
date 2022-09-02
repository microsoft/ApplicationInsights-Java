// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics;

/** Represents the result of a diagnosis process, including reporting failures. */
public interface DiagnosisResult<T extends Diagnosis> {

  T getDiagnosis();

  boolean succeeded();

  String getErrorMessage();
}
