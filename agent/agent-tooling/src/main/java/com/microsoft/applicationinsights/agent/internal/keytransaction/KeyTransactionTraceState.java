// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.keytransaction;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import io.opentelemetry.api.trace.TraceState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class KeyTransactionTraceState {

  // NOTE: dots are not valid in trace state keys, so we use underscores
  // example: "microsoft_kt=mykeytransaction:starttimemillis;mykeytransaction2:starttimemillis"
  static final String TRACE_STATE_KEY = "microsoft_kt";

  static Set<String> getKeyTransactionNames(TraceState traceState) {
    return getKeyTransactionNames(traceState.get(TRACE_STATE_KEY));
  }

  // visible for testing
  @SuppressWarnings("MixedMutabilityReturnType")
  static Set<String> getKeyTransactionNames(String value) {
    if (value == null) {
      return emptySet();
    }

    Set<String> names = new HashSet<>();
    for (String part : value.split(";")) {
      int index = part.lastIndexOf(':');
      if (index == -1) {
        // invalid format, ignore
        continue;
      }
      names.add(part.substring(0, index));
    }

    return names;
  }

  static Map<String, Long> getKeyTransactionStartTimes(TraceState traceState) {
    return getKeyTransactionStartTimes(traceState.get(TRACE_STATE_KEY));
  }

  // visible for testing
  @SuppressWarnings("MixedMutabilityReturnType")
  static Map<String, Long> getKeyTransactionStartTimes(String value) {
    if (value == null) {
      return emptyMap();
    }

    Map<String, Long> names = new HashMap<>();
    for (String part : value.split(";")) {
      int index = part.lastIndexOf(':');
      if (index == -1) {
        // invalid format, ignore
        continue;
      }
      String key = part.substring(0, index);
      long val;
      try {
        val = Long.parseLong(part.substring(index + 1));
      } catch (NumberFormatException e) {
        // invalid format, ignore
        continue;
      }
      names.put(key, val);
    }

    return names;
  }

  private KeyTransactionTraceState() {}
}
