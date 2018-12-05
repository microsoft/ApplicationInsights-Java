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
    public void testTraceparentAreResolved() {

        //setup
        Map<String, String> headers = new HashMap<>();
        Traceparent t = new Traceparent();
        headers.put(TraceContextCorrelation.TRACEPARENT_HEADER_NAME, t.toString());

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
    public void testCorrelationIdsAreResolvedIfNoTraceparentHeader() {

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
    public void testCorrelationIdsAreResolvedIfTraceparentEmpty() {

        //setup - empty RequestId
        Map<String, String> headers = new HashMap<>();
        headers.put(TraceContextCorrelation.TRACEPARENT_HEADER_NAME, "");

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
    public void testTracestateIsResolved() {
        Map<String, String> headers = new HashMap<>();
        String incomingTracestate = getTracestateHeaderValue("id1");
        headers.put(TraceContextCorrelation.TRACESTATE_HEADER_NAME, incomingTracestate);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        mockProfileFetcher.setAppIdToReturn("id2");

        TraceContextCorrelation.resolveRequestSource(request, requestTelemetry, "ikey1");

        Assert.assertEquals("cid-v1:id1", requestTelemetry.getSource());

    }

    @Test
    public void testSourceNotSetWhenIncomingAppIdInTraceStateIsSameAsCurrent() {
        Map<String, String> headers = new HashMap<>();
        String incomingTracestate = getTracestateHeaderValue("id1");
        headers.put(TraceContextCorrelation.TRACESTATE_HEADER_NAME, incomingTracestate);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        mockProfileFetcher.setAppIdToReturn("id1");

        TraceContextCorrelation.resolveRequestSource(request, requestTelemetry, "ikey1");
        //source and target have same appId
        Assert.assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testTracestateIsNotResolvedWhenHeaderNotPresent() {
        Map<String, String> headers = new HashMap<>();

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        mockProfileFetcher.setAppIdToReturn("id1");

        TraceContextCorrelation.resolveRequestSource(request, requestTelemetry, "ikey1");
        Assert.assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testTracestateIsNotResolvedIfHeaderIsEmpty() {
        Map<String, String> headers = new HashMap<>();
        headers.put(TraceContextCorrelation.TRACESTATE_HEADER_NAME, "");

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        mockProfileFetcher.setAppIdToReturn("id1");

        TraceContextCorrelation.resolveRequestSource(request, requestTelemetry, "ikey1");
        Assert.assertNull(requestTelemetry.getSource());
    }

    @Test(expected = AssertionError.class)
    public void testTraceStateIsNotResolvedIfHeaderDoesntHaveAzureComponent() {
        Map<String, String> headers = new HashMap<>();
        // get tracestate with non azure component
        String incomingTracestate = getTracestateHeaderValue(null);
        headers.put(TraceContextCorrelation.TRACESTATE_HEADER_NAME, incomingTracestate);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        mockProfileFetcher.setAppIdToReturn("id1");

        TraceContextCorrelation.resolveRequestSource(request, requestTelemetry, "ikey1");
        Assert.assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testTracestateIsNotResolvedWithNullIkey() {
        Map<String, String> headers = new HashMap<>();
        String incomingTracestate = getTracestateHeaderValue("id1");
        headers.put(TraceContextCorrelation.TRACESTATE_HEADER_NAME, incomingTracestate);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        mockProfileFetcher.setAppIdToReturn("id2");

        TraceContextCorrelation.resolveRequestSource(request, requestTelemetry, null);

        Assert.assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testTracestateIsNotResolvedWithNullRequestTelemetry() {
        Map<String, String> headers = new HashMap<>();
        String incomingTracestate = getTracestateHeaderValue("id1");
        headers.put(TraceContextCorrelation.TRACESTATE_HEADER_NAME, incomingTracestate);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        mockProfileFetcher.setAppIdToReturn("id2");

        TraceContextCorrelation.resolveRequestSource(request, null, "ikey1");

        Assert.assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testTracestateIsNotResolvedWithNullRequest() {
        Map<String, String> headers = new HashMap<>();
        String incomingTracestate = getTracestateHeaderValue("id1");
        headers.put(TraceContextCorrelation.TRACESTATE_HEADER_NAME, incomingTracestate);

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        mockProfileFetcher.setAppIdToReturn("id2");

        TraceContextCorrelation.resolveRequestSource(null, requestTelemetry, "ikey1");

        Assert.assertNull(requestTelemetry.getSource());
    }

     public static String getTracestateHeaderValue(String appId) {
        if (appId == null || appId.isEmpty()) {
            return "foo=bar";
        }
        return String.format("%s=cid-v1:%s", TraceContextCorrelation.AZURE_TRACEPARENT_COMPONENT_INITIAL, appId);
     }

    public static String getRequestSourceValue(String appId) {

        if (appId == null) {
            return "someValue";
        }

        return String.format("cid-v1:%s", appId);
    }
}
