/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.internal.channel.common;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.internal.channel.OldTelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.channel.OldTelemetrySerializer;
import com.microsoft.applicationinsights.internal.channel.TransmissionDispatcher;
import com.microsoft.applicationinsights.internal.channel.TransmissionsLoader;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.any;

public final class OldTransmitterImplTest {
    private final static String MOCK_WEB_CONTENT_TYPE = "MWCT";
    private final static String MOCK_CONTENT_ENCODING_TYPE = "MCET";

    @Test(expected = NullPointerException.class)
    public void testCtorWithNullTransmissionDispatcher() {
        OldTelemetrySerializer mockSerializer = Mockito.mock(OldTelemetrySerializer.class);
        TransmissionsLoader mockLoader = Mockito.mock(TransmissionsLoader.class);
        new OldTransmitterImpl(null, mockSerializer, mockLoader);
    }

    @Test(expected = NullPointerException.class)
    public void testCtorWithNullTelemetrySerializer() {
        TransmissionDispatcher mockDispatcher = Mockito.mock(TransmissionDispatcher.class);
        TransmissionsLoader mockLoader = Mockito.mock(TransmissionsLoader.class);
        new OldTransmitterImpl(mockDispatcher, null, mockLoader);
    }

    @Test(expected = NullPointerException.class)
    public void testCtorWithNullTransmissionsLoader() {
        TransmissionDispatcher mockDispatcher = Mockito.mock(TransmissionDispatcher.class);
        OldTelemetrySerializer mockSerializer = Mockito.mock(OldTelemetrySerializer.class);
        new OldTransmitterImpl(mockDispatcher, mockSerializer, null);
    }

    @Test
    public void testValidCtor() {
        OldTransmitterImpl transmitter = null;
        try {
            TransmissionDispatcher mockDispatcher = Mockito.mock(TransmissionDispatcher.class);
            OldTelemetrySerializer mockSerializer = Mockito.mock(OldTelemetrySerializer.class);
            TransmissionsLoader mockLoader = Mockito.mock(TransmissionsLoader.class);
            transmitter = new OldTransmitterImpl(mockDispatcher, mockSerializer, mockLoader);

            Mockito.verify(mockLoader, Mockito.times(1)).load(anyBoolean());
        } finally {
            if (transmitter != null) {
                transmitter.stop(1L, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    public void testScheduleSendWithNoTelemetries() {
        testScheduleSend(0, true);
        testScheduleSend(0, false);
    }

    @Test
    public void testScheduleSendWith1Telemetry() {
        testScheduleSend(1, true);
        testScheduleSend(1, false);
    }

    @Test
    public void testScheduleSendWith100Telemetries() {
        testScheduleSend(100, true);
        testScheduleSend(100, false);
    }

    @Test
    public void testSendNowWithNoTelemetries() {
        testSendNow(0, true);
        testSendNow(0, false);
    }

    @Test
    public void testSendNowWith1Telemetry() {
        testSendNow(1, true);
        testSendNow(1, false);
    }

    @Test
    public void testSendNowWith100Telemetries() {
        testSendNow(100, true);
        testSendNow(100, false);
    }

    private void testSendNow(int numberOfTransmissions, boolean serializeOk) {
        OldTransmitterImpl transmitter = null;
        try {
            TransmissionDispatcher mockDispatcher = Mockito.mock(TransmissionDispatcher.class);
            TransmissionsLoader mockLoader = Mockito.mock(TransmissionsLoader.class);

            final List<Telemetry> telemetries = new ArrayList<Telemetry>();
            for (int i = 0; i < numberOfTransmissions; ++i) {
                telemetries.add(new Telemetry() {
                    @Override
                    public Date getTimestamp() {
                        return null;
                    }

                    @Override
                    public void setTimestamp(Date date) {

                    }

                    @Override
                    public TelemetryContext getContext() {
                        return null;
                    }

                    @Override
                    public Map<String, String> getProperties() {
                        return null;
                    }

                    @Override
                    public void sanitize() {

                    }

                    @Override
                    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
                        writer.write("ver", 1);
                        writer.write("telemetryName", "telemetryName");
                    }
                });
            }

            Transmission mockTransmission = new Transmission(new byte[1], MOCK_WEB_CONTENT_TYPE, MOCK_CONTENT_ENCODING_TYPE);
            Optional<Transmission> mockSerialize = Optional.absent();
            if (serializeOk) {
                mockSerialize = Optional.of(mockTransmission);
            }
            OldTelemetrySerializer mockSerializer = Mockito.mock(OldTelemetrySerializer.class);
            Mockito.doReturn(mockSerialize).when(mockSerializer).serialize(telemetries);

            transmitter = new OldTransmitterImpl(mockDispatcher, mockSerializer, mockLoader);

            transmitter.sendNow(telemetries);
            Thread.sleep(1000);

            if (numberOfTransmissions == 0) {
                Mockito.verify(mockSerializer, Mockito.never()).serialize(telemetries);
                Mockito.verify(mockDispatcher, Mockito.never()).dispatch(any(Transmission.class));
            } else {
                Mockito.verify(mockSerializer, Mockito.times(1)).serialize(telemetries);
                if (serializeOk) {
                    Mockito.verify(mockDispatcher, Mockito.times(1)).dispatch(any(Transmission.class));
                } else {
                    Mockito.verify(mockDispatcher, Mockito.never()).dispatch(any(Transmission.class));
                }
            }
        } catch (InterruptedException e) {
        } finally {
            if (transmitter != null) {
                transmitter.stop(1L, TimeUnit.SECONDS);
            }
        }
    }

    private void testScheduleSend(int numberOfTransmissions, boolean serializeOk) {
        OldTransmitterImpl transmitter = null;
        try {
            TransmissionDispatcher mockDispatcher = Mockito.mock(TransmissionDispatcher.class);
            TransmissionsLoader mockLoader = Mockito.mock(TransmissionsLoader.class);

            final List<Telemetry> telemetries = new ArrayList<Telemetry>();
            for (int i = 0; i < numberOfTransmissions; ++i) {
                telemetries.add(new Telemetry() {
                    @Override
                    public Date getTimestamp() {
                        return null;
                    }

                    @Override
                    public void setTimestamp(Date date) {

                    }

                    @Override
                    public TelemetryContext getContext() {
                        return null;
                    }

                    @Override
                    public Map<String, String> getProperties() {
                        return null;
                    }

                    @Override
                    public void sanitize() {

                    }

                    @Override
                    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {

                    }
                });
            }

            Transmission mockTransmission = new Transmission(new byte[1], MOCK_WEB_CONTENT_TYPE, MOCK_CONTENT_ENCODING_TYPE);
            Optional<Transmission> mockSerialize = Optional.absent();
            if (serializeOk) {
                mockSerialize = Optional.of(mockTransmission);
            }
            OldTelemetrySerializer mockSerializer = Mockito.mock(OldTelemetrySerializer.class);
            Mockito.doReturn(mockSerialize).when(mockSerializer).serialize(telemetries);

            OldTelemetriesTransmitter.TelemetriesFetcher mockFetcher = Mockito.mock(OldTelemetriesTransmitter.TelemetriesFetcher.class);
            Mockito.doReturn(telemetries).when(mockFetcher).fetch();

            transmitter = new OldTransmitterImpl(mockDispatcher, mockSerializer, mockLoader);

            transmitter.scheduleSend(mockFetcher, 100L, TimeUnit.MICROSECONDS);
            Thread.sleep(1000);

            Mockito.verify(mockFetcher, Mockito.times(1)).fetch();
            if (numberOfTransmissions == 0) {
                Mockito.verify(mockSerializer, Mockito.never()).serialize(telemetries);
                Mockito.verify(mockDispatcher, Mockito.never()).dispatch(any(Transmission.class));
            } else {
                Mockito.verify(mockSerializer, Mockito.times(1)).serialize(telemetries);
                if (serializeOk) {
                    Mockito.verify(mockDispatcher, Mockito.times(1)).dispatch(any(Transmission.class));
                } else {
                    Mockito.verify(mockDispatcher, Mockito.never()).dispatch(any(Transmission.class));
                }
            }
        } catch (InterruptedException e) {
        } finally {
            if (transmitter != null) {
                transmitter.stop(1L, TimeUnit.SECONDS);
            }
        }
    }
}
