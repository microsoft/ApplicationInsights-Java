package com.microsoft.applicationinsights.web.internal.correlation;

import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.correlation.mocks.MockProfileFetcher;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Traceparent;
import com.microsoft.applicationinsights.web.utils.ServletUtils;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TraceContextCorrelationTests {

    private static MockProfileFetcher mockProfileFetcher;

    @Before
    public void testInitialize() {

        // initialize mock profile fetcher (for resolving ikeys to appIds)
        mockProfileFetcher = new MockProfileFetcher();
        InstrumentationKeyResolver.INSTANCE.setProfileFetcher(mockProfileFetcher);
        InstrumentationKeyResolver.INSTANCE.clearCache();
    }

    @Test
    public void testCorrelationIdsAreResolved() {

        //setup
        Map<String, String> headers = new HashMap<>();
        Traceparent t = new Traceparent();
        headers.put(TraceContextCorrelation.CORRELATION_HEADER_NAME, t.toString());

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TraceContextCorrelation.resolveCorrelation(request, response, requestTelemetry);

        //validate we have generated proper ID's
        Assert.assertNotNull(requestTelemetry.getId());

        Assert.assertTrue(requestTelemetry.getId().startsWith(t.getTraceId()));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        Assert.assertEquals(t.getTraceId(), operation.getId());
        Assert.assertEquals(t.getTraceId() + "-" + t.getSpanId(), operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedIfNoTraceIdHeader() {

        //setup - no headers
        Map<String, String> headers = new HashMap<>();
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TraceContextCorrelation.resolveCorrelation(request, response, requestTelemetry);

        //validate operation context ID's - there is no parent, so parentId should be null, traceId
        // is newly generated and request.Id is based on new traceId-spanId
        OperationContext operation = requestTelemetry.getContext().getOperation();

        Assert.assertNotNull(requestTelemetry.getId());

        // First trace will have it's own spanId also.
        Assert.assertTrue(requestTelemetry.getId().startsWith(operation.getId()+"-"));
        Assert.assertNull(operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedIfRequestIdEmpty() {

        //setup - empty RequestId
        Map<String, String> headers = new HashMap<>();
        headers.put(TraceContextCorrelation.CORRELATION_HEADER_NAME, "");

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TraceContextCorrelation.resolveCorrelation(request, response, requestTelemetry);

        //validate operation context ID's - there is no parent, so parentId should be null, traceId
        // is newly generated and request.Id is based on new traceId-spanId
        OperationContext operation = requestTelemetry.getContext().getOperation();

        Assert.assertNotNull(requestTelemetry.getId());
        // First trace will have it's own spanId also.
        Assert.assertTrue(requestTelemetry.getId().startsWith(operation.getId()+"-"));
        Assert.assertNull(operation.getParentId());
    }
}
