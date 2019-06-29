/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.sdk;

import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface SdkBridge<T> {

    void bindRequestTelemetryContext(T requestTelemetryContext);

    void unbindRequestTelemetryContext();

    String generateChildDependencyTarget(String requestContext, boolean w3c);

    <C> String propagate(Setter<C> setter, C carrier, boolean w3c, boolean w3cBackCompat);

    void track(RemoteDependencyTelemetry telemetry);

    void track(TraceTelemetry telemetry);

    void track(ExceptionTelemetry telemetry);

    // hides instrumentation api (e.g. so it can be shaded)
    class Setter<C> implements org.glowroot.instrumentation.api.Setter<C> {

        private final org.glowroot.instrumentation.api.Setter<C> setter;

        public Setter(org.glowroot.instrumentation.api.Setter<C> setter) {
            this.setter = setter;
        }

        public void put(C carrier, String key, String value) {
            setter.put(carrier, key, value);
        }
    }

    class TraceTelemetry {

        private final String message;

        private final @Nullable String level;

        private final Map<String, String> properties = new HashMap<>();

        public TraceTelemetry(String message, @Nullable String level) {
            this.message = message;
            this.level = level;
        }

        public String getMessage() {
            return message;
        }

        public @Nullable String getLevel() {
            return level;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }

    class ExceptionTelemetry {

        private final Throwable throwable;

        private final @Nullable String level;

        private final Map<String, String> properties = new HashMap<>();

        public ExceptionTelemetry(Throwable throwable) {
            this(throwable, null);
        }

        public ExceptionTelemetry(Throwable throwable, @Nullable String level) {
            this.throwable = throwable;
            this.level = level;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public  @Nullable String getLevel() {
            return level;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }

    class RemoteDependencyTelemetry {

        private final long timestamp;
        private final long durationMillis;

        private final String type;
        private final boolean success;

        private @Nullable String id;
        private @Nullable String name;
        private @Nullable String commandName;
        private @Nullable String target;
        private @Nullable String resultCode;

        private final Map<String, String> properties = new HashMap<>();

        public RemoteDependencyTelemetry(long timestamp, long durationMillis, String type, boolean success) {
            this.timestamp = timestamp;
            this.durationMillis = durationMillis;
            this.type = type;
            this.success = success;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getDurationMillis() {
            return durationMillis;
        }

        public String getType() {
            return type;
        }

        public boolean isSuccess() {
            return success;
        }

        public @Nullable String getId() {
            return id;
        }

        public void setId(@Nullable String id) {
            this.id = id;
        }

        public @Nullable String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public @Nullable String getCommandName() {
            return commandName;
        }

        public void setCommandName(String commandName) {
            this.commandName = commandName;
        }

        public @Nullable String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public @Nullable String getResultCode() {
            return resultCode;
        }

        public void setResultCode(String resultCode) {
            this.resultCode = resultCode;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }
}
