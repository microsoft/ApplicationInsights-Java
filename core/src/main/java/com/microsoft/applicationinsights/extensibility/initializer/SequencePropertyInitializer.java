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

package com.microsoft.applicationinsights.extensibility.initializer;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.telemetry.Telemetry;

import org.apache.commons.codec.binary.Base64;

import com.google.common.base.Strings;

/**
 * An {@link com.microsoft.applicationinsights.extensibility.TelemetryInitializer} implementation that
 * populates Sequence property to ensure correct ordering of the telemetry in the portal.
 */
public final class SequencePropertyInitializer implements TelemetryInitializer {
    private final static String SEPARATOR = ":";

    private final String stablePrefix;
    private final AtomicLong currentNumber = new AtomicLong(-1);

    public SequencePropertyInitializer() {
        stablePrefix = uuidToBase64() + SEPARATOR;
    }

    /**
     * Sets the Telemetry's sequence if there that sequence is null or empty.
     * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} to initialize.
     */
    @Override
    public void initialize(Telemetry telemetry) {
        String sequence = telemetry.getSequence();
        if (Strings.isNullOrEmpty(sequence)) {
            sequence = stablePrefix + String.valueOf(currentNumber.incrementAndGet());
            telemetry.setSequence(sequence);
        }
    }

    private static String uuidToBase64() {
        Base64 base64 = new Base64();
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return base64.encodeBase64URLSafeString(bb.array());
    }
}
