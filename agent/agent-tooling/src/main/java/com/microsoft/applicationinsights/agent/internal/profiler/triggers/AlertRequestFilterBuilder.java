// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.alerting.analysis.filter.AlertRequestFilter;

class AlertRequestFilterBuilder {

  static AlertRequestFilter build(Configuration.RequestFilter filter) {
    if (filter.type == Configuration.RequestFilterType.NAME_REGEX) {
      return new AlertRequestFilter.RegexRequestNameFilter(filter.value);
    }

    throw new IllegalStateException("Unexpected filter type: " + filter.type);
  }

  private AlertRequestFilterBuilder() {}
}
