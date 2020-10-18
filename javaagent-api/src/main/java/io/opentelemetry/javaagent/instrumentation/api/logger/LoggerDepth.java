/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.api.logger;

// this is a marker class that can be used across different logging instrumentations in order to
// prevent nested log capture (this is especially a problem on JBoss)
public class LoggerDepth {}
