// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JFR Service Profiler main entry point, wires up the items below.
 *
 * <ul>
 *   <li>Configuration polling
 *   <li>Notifying upstream
 *   <li>consumers (such as the alerting subsystem) of configuration updates
 *   <li>JFR Profiling service
 *   <li>JFR Uploader service
 * </ul>
 */
class ProfilerInitializer {

  private static final Logger logger = LoggerFactory.getLogger(ProfilerInitializer.class);

  private ProfilerInitializer() {}
}
