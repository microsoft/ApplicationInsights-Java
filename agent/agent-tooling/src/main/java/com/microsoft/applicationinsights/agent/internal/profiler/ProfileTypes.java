// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

enum ProfileTypes {
  PROFILE,

  // Same as profile with some potentially sensitive data removed, such as environment variables
  PROFILE_WITHOUT_ENV_DATA,

  // Enables the events needed to perform a performance diagnosis
  DIAGNOSTIC_PROFILE
}
