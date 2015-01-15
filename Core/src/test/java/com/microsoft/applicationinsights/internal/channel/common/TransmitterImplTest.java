package com.microsoft.applicationinsights.internal.channel.common;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.channel.TelemetrySerializer;
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

public final class TransmitterImplTest {
    private final static String MOCK_WEB_CONTENT_TYPE = "MWCT";
    private final static String MOCK_CONTENT_ENCODING_TYPE = "MCET";

    @Test(expected = NullPointerException.class)
    public void testCtorWithNullTransmissionDispatcher() {
        TelemetrySerializer mockSerializer = Mockito.mock(TelemetrySerializer.class);
        TransmissionsLoader mockLoader = Mockito.mock(TransmissionsLoader.class);
        new TransmitterImpl(null, mockSerializer, mockLoader);
    }

    @Test(expected = NullPointerException.class)
    public void testCtorWithNullTelemetrySerializer() {
        TransmissionDispatcher mockDispatcher = Mockito.mock(TransmissionDispatcher.class);
        TransmissionsLoader mockLoader = Mockito.mock(TransmissionsLoader.class);
        new TransmitterImpl(mockDispatcher, null, mockLoader);
    }

    @Test(expected = NullPointerException.class)
    public void testCtorWithNullTransmissionsLoader() {
        TransmissionDispatcher mockDispatcher = Mockito.mock(TransmissionDispatcher.class);
        TelemetrySerializer mockSerializer = Mockito.mock(TelemetrySerializer.class);
        new TransmitterImpl(mockDispatcher, mockSerializer, null);
    }

    @Test
    public void testValidCtor() {
        TransmitterImpl transmitter = null;
        try {
            TransmissionDispatcher mockDispatcher = Mockito.mock(TransmissionDispatcher.class);
            TelemetrySerializer mockSerializer = Mockito.mock(TelemetrySerializer.class);
            TransmissionsLoader mockLoader = Mockito.mock(TransmissionsLoader.class);
            transmitter = new TransmitterImpl(mockDispatcher, mockSerializer, mockLoader);

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
        TransmitterImpl transmitter = null;
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
            TelemetrySerializer mockSerializer = Mockito.mock(TelemetrySerializer.class);
            Mockito.doReturn(mockSerialize).when(mockSerializer).serialize(telemetries);

            transmitter = new TransmitterImpl(mockDispatcher, mockSerializer, mockLoader);

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
        TransmitterImpl transmitter = null;
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
            TelemetrySerializer mockSerializer = Mockito.mock(TelemetrySerializer.class);
            Mockito.doReturn(mockSerialize).when(mockSerializer).serialize(telemetries);

            TelemetriesTransmitter.TelemetriesFetcher mockFetcher = Mockito.mock(TelemetriesTransmitter.TelemetriesFetcher.class);
            Mockito.doReturn(telemetries).when(mockFetcher).fetch();

            transmitter = new TransmitterImpl(mockDispatcher, mockSerializer, mockLoader);

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
