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

package com.microsoft.applicationinsights.web.extensibility.modules;

import org.apache.http.HttpStatus;
import org.eclipse.jetty.http.HttpMethods;
import org.junit.*;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import com.microsoft.applicationinsights.web.utils.HttpHelper;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.web.utils.JettyTestServer;
import com.microsoft.applicationinsights.web.utils.MockTelemetryChannel;
import com.microsoft.applicationinsights.web.utils.ServletUtils;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtils;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtilsTests;
import com.microsoft.applicationinsights.web.internal.correlation.InstrumentationKeyResolver;
import com.microsoft.applicationinsights.web.internal.correlation.ProfileFetcherResultTaskStatus;
import com.microsoft.applicationinsights.web.internal.correlation.mocks.MockProfileFetcher;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Created by yonisha on 2/2/2015.
 */
public class WebRequestTrackingTelemetryModuleTests {
    private static final String DEFAULT_REQUEST_URI = "/controller/action.action";
    private static final String DEFAULT_REQUEST_NAME = HttpMethods.GET + " " + DEFAULT_REQUEST_URI;

    private static JettyTestServer server = new JettyTestServer();
    private static WebRequestTrackingTelemetryModule defaultModule;
    private static MockTelemetryChannel channel;
    private static MockProfileFetcher mockProfileFetcher;

    // region Initialization

    @BeforeClass
    public static void classInitialize() throws Exception {
        server.start();

        // Set mock channel
        channel = MockTelemetryChannel.INSTANCE;
        TelemetryConfiguration.getActive().setChannel(channel);
        TelemetryConfiguration.getActive().setInstrumentationKey("SOME_INT_KEY");
    }

    @Before
    public void testInitialize() {

        // initialize mock profile fetcher (for resolving ikeys to appIds)
        mockProfileFetcher = new MockProfileFetcher();
        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.PENDING);
        InstrumentationKeyResolver.INSTANCE.setProfileFetcher(mockProfileFetcher);
        InstrumentationKeyResolver.INSTANCE.clearCache();

        defaultModule = new WebRequestTrackingTelemetryModule();
        defaultModule.initialize(TelemetryConfiguration.getActive());

        channel.reset();
    }

    @AfterClass
    public static void classCleanup() throws Exception {
        server.shutdown();
    }

    // endregion Initialization

    // region Tests

    @Test
    public void testHttpRequestTrackedSuccessfully() throws Exception {
        HttpHelper.sendRequestAndGetResponseCookie(server.getPortNumber());

        List<RequestTelemetry> items = channel.getTelemetryItems(RequestTelemetry.class);
        assertEquals(1, items.size());
        RequestTelemetry requestTelemetry = items.get(0);

        assertEquals(String.valueOf(HttpStatus.SC_OK), requestTelemetry.getResponseCode());
        assertEquals(HttpMethods.GET + " /", requestTelemetry.getName());
        assertEquals(HttpMethods.GET, requestTelemetry.getHttpMethod());
        assertEquals("http://localhost:" + server.getPortNumber() + "/", requestTelemetry.getUrl().toString());
    }

    @Test
    public void testResponseHeaderIsSetForRequestContext() throws Exception {
        
        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        mockProfileFetcher.setAppIdToReturn("myId");

        Map<String, List<String>> headers = HttpHelper.sendRequestAndGetHeaders(server.getPortNumber());
        List<String> requestContextValues = headers.get(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME);
        Assert.assertNotNull(requestContextValues);
        Assert.assertTrue(requestContextValues.size() == 1);

        String requestContext = requestContextValues.get(0);
        Assert.assertEquals("appId=cid-v1:myId", requestContext);
    }
   
    @Test
    public void testOnBeginRequestCatchAllExceptions() {
        ServletRequest request = createFaultyServletRequestMock();

        defaultModule.onBeginRequest(request, null);
    }

    @Test
    public void testOnEndRequestCatchAllExceptions() {
        ServletRequest request = createFaultyServletRequestMock();

        defaultModule.onEndRequest(request, null);
    }

    @Test
    public void testRequestNameCalculationWithoutQueryString() {
        testRequestNameCalculationWithGivenQueryString(null, null);
    }

    @Test
    public void testRequestNameCalculationWithQueryString() {
        testRequestNameCalculationWithGivenQueryString("?param1=value1;param2=value2", null);
    }

    @Test
    public void testRequestNameCalculationWithJSessionId() {
        testRequestNameCalculationWithGivenQueryString("", ";jsessionid=D59C79DF9A2C81E931CD67659AC01D17");
    }

    @Test
    public void testUserAgentIsBeingSet() throws Exception {
        HttpHelper.sendRequestAndGetResponseCookie(server.getPortNumber());

        List<RequestTelemetry> items = channel.getTelemetryItems(RequestTelemetry.class);
        assertEquals(1, items.size());
        RequestTelemetry requestTelemetry = items.get(0);

        Assert.assertEquals(HttpHelper.TEST_USER_AGENT, requestTelemetry.getContext().getUser().getUserAgent());
    }

    @Test
    public void testCrossComponentCorrelationHeadersAreCaptured() {
        
        //setup: initialize a request telemetry context
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        //mock a servlet request with cross-component correlation headers
        Map<String, String> headers = new HashMap<String, String>();
        String incomingId = "|guid.bcec871c_1.";

        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, TelemetryCorrelationUtilsTests.getRequestContextHeaderValue("id1", null));
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers, 1);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        //configure mock appId fetcher to return different appId from what's on the request header
        mockProfileFetcher.setAppIdToReturn("id2");
        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);

        //run
        defaultModule.onBeginRequest(request, response);

        // verify ID's are set as expected in request telemetry 
        RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        Assert.assertNotNull(requestTelemetry.getId());
        Assert.assertEquals(incomingId.length() + 9, requestTelemetry.getId().length());
        Assert.assertTrue(requestTelemetry.getId().startsWith(incomingId));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        Assert.assertEquals("guid", operation.getId());
        Assert.assertEquals(incomingId, operation.getParentId());
        
        //validate custom properties
        Assert.assertEquals(2, requestTelemetry.getProperties().size());
        Assert.assertEquals("value1", requestTelemetry.getProperties().get("key1"));
        Assert.assertEquals("value2", requestTelemetry.getProperties().get("key2"));

        //run onEnd
        defaultModule.onEndRequest(request, null);

        //validate source
        Assert.assertEquals(TelemetryCorrelationUtilsTests.getRequestSourceValue("id1", null), requestTelemetry.getSource());
    }

    @Test
    public void testTelemetryCreatedWithinRequestScopeIsRequestChild() {
        
        //setup: initialize a request context
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        //mock a servlet request with cross-component correlation headers
        Map<String, String> headers = new HashMap<String, String>();
        
        String incomingId = "|guid.bcec871c_1.";
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);

        String correlationContext = "key1=value1, key2=value2";
        headers.put(TelemetryCorrelationUtils.CORRELATION_CONTEXT_HEADER_NAME, correlationContext);

        ServletRequest request = ServletUtils.createServletRequestWithHeaders(headers, 1);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        //run module
        defaultModule.onBeginRequest(request, response);

        //additional telemetry is manually tracked
        TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.trackException(new Exception());

        List<ExceptionTelemetry> items = channel.getTelemetryItems(ExceptionTelemetry.class);
        Assert.assertEquals(1, items.size());
        ExceptionTelemetry exceptionTelemetry = items.get(0);

        RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();

        //validate manually tracked telemetry is a child of the request telemetry
        Assert.assertEquals("guid", exceptionTelemetry.getContext().getOperation().getId());
        Assert.assertEquals(requestTelemetry.getId(), exceptionTelemetry.getContext().getOperation().getParentId());
        Assert.assertEquals(2, exceptionTelemetry.getProperties().size());
        Assert.assertEquals("value1", exceptionTelemetry.getProperties().get("key1"));
        Assert.assertEquals("value2", exceptionTelemetry.getProperties().get("key2"));
    }

    @Test
    public void testOnEndAddsSourceFieldForRequestWithRequestContext() {
        
        //setup: initialize a request telemetry context
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        //mock a servlet request with cross-component correlation headers
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, TelemetryCorrelationUtilsTests.getRequestContextHeaderValue("id1", null));
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        //configure mock appId fetcher to return different appId from what's on the request header
        mockProfileFetcher.setAppIdToReturn("id2");
        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);

        //run
        defaultModule.onBeginRequest(request, response);
        defaultModule.onEndRequest(request, null);

        //validate source
        RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        Assert.assertEquals(TelemetryCorrelationUtilsTests.getRequestSourceValue("id1", null), requestTelemetry.getSource());
    }

    @Test
    public void testOnEndDoesNotAddSourceFieldForRequestFromSameApp() {
        
        //setup: initialize a request telemetry context
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        //mock a servlet request with cross-component correlation headers
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, TelemetryCorrelationUtilsTests.getRequestContextHeaderValue("id1", null));
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        //configure mock appId fetcher to return the same appId from what's on the request header
        mockProfileFetcher.setAppIdToReturn("id1");
        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);

        //run
        defaultModule.onBeginRequest(request, response);
        defaultModule.onEndRequest(request, null);

        //validate source
        RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        Assert.assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testOnEndAddsSourceFieldForRequestWithRoleNameOnly() {
        
        //setup: initialize a request telemetry context
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        //mock a servlet request with cross-component correlation headers
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, TelemetryCorrelationUtilsTests.getRequestContextHeaderValue(null, "Front End"));
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        //configure mock appId fetcher to return different appId from what's on the request header
        mockProfileFetcher.setAppIdToReturn("id2");
        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);

        //run
        defaultModule.onBeginRequest(request, response);
        defaultModule.onEndRequest(request, null);

        //validate source
        RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        Assert.assertEquals(TelemetryCorrelationUtilsTests.getRequestSourceValue(null, "Front End"), requestTelemetry.getSource());
    }

    @Test
    public void testOnEndAddsSourceFieldForRequestWithRoleNameAndAppId() {
        
        //setup: initialize a request telemetry context
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        //mock a servlet request with cross-component correlation headers
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, TelemetryCorrelationUtilsTests.getRequestContextHeaderValue("id1", "Front End"));
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        //configure mock appId fetcher to return different appId from what's on the request header
        mockProfileFetcher.setAppIdToReturn("id2");
        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);

        //run
        defaultModule.onBeginRequest(request, response);
        defaultModule.onEndRequest(request, null);

        //validate source
        RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        Assert.assertEquals(TelemetryCorrelationUtilsTests.getRequestSourceValue("id1", "Front End"), requestTelemetry.getSource());
    }

    @Test
    public void testOnEndAddsSourceFieldForRequestWithRoleNameForSameApp() {
        
        //setup: initialize a request telemetry context
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        //mock a servlet request with cross-component correlation headers
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, TelemetryCorrelationUtilsTests.getRequestContextHeaderValue("id1", "Front End"));
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        //configure mock appId fetcher to return the same appId from what's on the request header
        mockProfileFetcher.setAppIdToReturn("id1");
        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);

        //run
        defaultModule.onBeginRequest(request, response);
        defaultModule.onEndRequest(request, null);

        //validate source
        RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        Assert.assertEquals(TelemetryCorrelationUtilsTests.getRequestSourceValue(null, "Front End"), requestTelemetry.getSource());
    }

    @Test
    public void testOnEndDoesNotOverrideSourceField() {
        
        //setup: initialize a request telemetry context
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);
        RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        requestTelemetry.setSource("myAppId");

        //mock a servlet request with cross-component correlation headers
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, TelemetryCorrelationUtilsTests.getRequestContextHeaderValue("id1", "Front End"));
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        //configure mock appId fetcher to return different appId from what's on the request header
        mockProfileFetcher.setAppIdToReturn("id2");
        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);

        //run
        defaultModule.onBeginRequest(request, response);
        defaultModule.onEndRequest(request, null);

        //validate source
        Assert.assertEquals("myAppId", requestTelemetry.getSource());
    }

    @Test
    public void testInstrumentationKeyIsResolvedDuringModuleInit() {
        
        // module is initialized during test init, so at this point we should 
        // already have a task in pending status. This means the fetcher has already
    	// been called once.
    	Assert.assertEquals(1, mockProfileFetcher.callCount());
        String ikey = TelemetryConfiguration.getActive().getInstrumentationKey();

        //calling resolver now will actually retrieve the appId from the completed task 
        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        mockProfileFetcher.setAppIdToReturn("someAppId");
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(ikey);
        Assert.assertEquals(2, mockProfileFetcher.callCount());
        Assert.assertEquals("cid-v1:someAppId", appId);
        
        //calling it again should retrieve appId from cache (i.e. fetcher call count remains 2)
        appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(ikey);
        Assert.assertEquals(2, mockProfileFetcher.callCount());
        Assert.assertEquals("cid-v1:someAppId", appId);
    }

    @Test
    public void testInstrumentationKeyIsResolvedIfModifiedAtRuntime() {
        
    	// before request begins, resolving has been kicked-off during init
        Assert.assertEquals(1, mockProfileFetcher.callCount());
        
        //setup: initialize a request telemetry context
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        //mock a servlet request with cross-component correlation headers
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, TelemetryCorrelationUtilsTests.getRequestContextHeaderValue("id1", null));
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        //configure mock appId fetcher to return different appId from what's on the request header
        mockProfileFetcher.setAppIdToReturn("id2");
        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);

        //run
        defaultModule.onBeginRequest(request, response);
        
        //onBegin must have called fetcher
        Assert.assertEquals(2, mockProfileFetcher.callCount());

        // mimic customer modifying ikey at runtime in request handler (e.g. controller)
        TelemetryConfiguration.getActive().setInstrumentationKey("myOtherIkey");
        
        // module.onEndRequest must detect change and start resolving new ikey
        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.PENDING);
        mockProfileFetcher.setAppIdToReturn("id3");
        defaultModule.onEndRequest(request, null);
        
        //the ikey is new, which means its appId ("id3") is not in cache, so again we call the fetcher
        Assert.assertEquals(3, mockProfileFetcher.callCount());

        // at this point source won't be set yet because the ikey has changed and so a new resolve task has started
        RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        Assert.assertNull(requestTelemetry.getSource());

        //another request comes in
        RequestTelemetryContext context2 = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context2);
        ServletRequest request2 = ServletUtils.createServletRequestWithHeaders(headers);
        
        mockProfileFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        
        defaultModule.onBeginRequest(request2, response);

        // at this point, the new appId should be available in the cache
        Assert.assertEquals(4, mockProfileFetcher.callCount());
        
        defaultModule.onEndRequest(request, null);
        Assert.assertEquals(4, mockProfileFetcher.callCount());

        RequestTelemetry requestTelemetry2 = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        Assert.assertEquals("cid-v1:id1", requestTelemetry2.getSource());

        // if another request comes in, it should retrieve appId from cache
        RequestTelemetryContext context3 = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context3);
        ServletRequest request3 = ServletUtils.createServletRequestWithHeaders(headers);
        defaultModule.onBeginRequest(request3, response);
        Assert.assertEquals(4, mockProfileFetcher.callCount());
        
        // module.onEndRequest will attempt to retrieve new appId from task if it is completed
        defaultModule.onEndRequest(request, null);
        Assert.assertEquals(4, mockProfileFetcher.callCount());
        RequestTelemetry requestTelemetry3 = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        Assert.assertEquals("cid-v1:id1", requestTelemetry3.getSource());
    }

    // endregion Tests

    // region Private methods

    private void testRequestNameCalculationWithGivenQueryString(String queryString, String pathVariable) {
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        ServletRequest servletRequest = createServletRequest(queryString, pathVariable);
        defaultModule.onBeginRequest(servletRequest, null);

        RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        Assert.assertEquals("Request name not valid.", DEFAULT_REQUEST_NAME, requestTelemetry.getName());
    }

    private ServletRequest createServletRequest(String queryString, String pathVariable) {
        HttpServletRequest request = mock(HttpServletRequest.class);

        String uri = DEFAULT_REQUEST_URI;
        if (pathVariable != null) {
            uri = uri.concat(pathVariable);
        }

        when(request.getRequestURI()).thenReturn(uri);
        when(request.getMethod()).thenReturn(HttpMethods.GET);
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader("Host")).thenReturn("localhost:" + server.getPortNumber());
        when(request.getQueryString()).thenReturn(queryString);

        return request;
    }   

    private ServletRequest createFaultyServletRequestMock() {
        ServletRequest request = mock(ServletRequest.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new Exception("FATAL!");
            }
        }).when(request).getScheme();

        return request;
    }

    // endregion Private methods
}