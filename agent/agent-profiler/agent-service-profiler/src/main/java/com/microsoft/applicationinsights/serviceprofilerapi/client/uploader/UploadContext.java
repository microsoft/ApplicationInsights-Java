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

package com.microsoft.applicationinsights.serviceprofilerapi.client.uploader;

import java.io.File;
import java.util.UUID;

/**
 * {@code UploadContext} class represents parameters for trace file upload operation.
 * <p>
 * This class is intended for internal Java profiler use.
 */
public class UploadContext {
    private final UUID dataCube;
    private final long sessionId;
    private final File traceFile;
    private final UUID profileId;
    private final String machineName;

    public UploadContext(String machineName, UUID dataCube, long sessionId,
                         File traceFile, UUID profileId) {
        this.machineName = machineName;
        this.dataCube = dataCube;
        this.sessionId = sessionId;
        this.traceFile = traceFile;
        this.profileId = profileId;
    }

    public String getMachineName() {
        return machineName;
    }

    public File getTraceFile() {
        return traceFile;
    }

    public long getSessionId() {
        return sessionId;
    }

    public UUID getDataCube() {
        return dataCube;
    }

    public UUID getProfileId() {
        return profileId;
    }
}