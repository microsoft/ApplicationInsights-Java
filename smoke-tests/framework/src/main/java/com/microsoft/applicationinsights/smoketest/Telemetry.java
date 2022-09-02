// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;

public class Telemetry {
  public Envelope rdEnvelope;
  public Envelope rddEnvelope1;
  public Envelope rddEnvelope2;
  public Envelope rddEnvelope3;

  public RequestData rd;
  public RemoteDependencyData rdd1;
  public RemoteDependencyData rdd2;
  public RemoteDependencyData rdd3;
}
