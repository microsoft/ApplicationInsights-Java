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

package com.microsoft.applicationinsights.web.internal.correlation;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.junit.*;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.utils.ServletUtils;
import java.util.Hashtable;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class TelemetryCorrelationUtilsTests {

    private ApplicationIdResolver mockProfileFetcher;

    @Before
    public void testInitialize() {
        // initialize mock profile fetcher (for resolving ikeys to appIds)
        mockProfileFetcher = mock(ApplicationIdResolver.class);
        InstrumentationKeyResolver.INSTANCE.setAppIdResolver(mockProfileFetcher);
        InstrumentationKeyResolver.INSTANCE.clearCache();
    }

    @After
    public void tearDown() {
        mockProfileFetcher = null;
        InstrumentationKeyResolver.INSTANCE.setAppIdResolver(new CdsProfileFetcher());
    }

    @Test
    public void testIsHierarchicalIdValidCase() {

        //setup
        String id = "|9e74f0e5-efc4-41b5-86d1-3524a43bd891.bcec871c_1.";

        //validate
        assertTrue(TelemetryCorrelationUtils.isHierarchicalId(id));
    }

    @Test
    public void testIsHierarchicalIdEmpty() {

        //setup
        String id = "";

        //validate
        assertFalse(TelemetryCorrelationUtils.isHierarchicalId(id));
    }

    @Test
    public void testIsHierarchicalIdNull() {

        //setup
        String id = null;

        //validate
        assertFalse(TelemetryCorrelationUtils.isHierarchicalId(id));
    }

    @Test
    public void testIsHierarchicalIdInvalidCase() {

        //setup
        String id = "9e74f0e5-efc4-41b5-86d1-3524a43bd891.bcec871c_1.";

        //validate
        assertFalse(TelemetryCorrelationUtils.isHierarchicalId(id));
    }

    @Test
    public void testCorrelationIdsAreResolved() {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String rootId = "9e74f0e5-efc4-41b5-86d1-3524a43bd891";
        String incomingId = "|9e74f0e5-efc4-41b5-86d1-3524a43bd891.bcec871c_1.";
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);

        //validate we have generated proper ID's
        assertNotNull(requestTelemetry.getId());
        assertEquals(incomingId.length() + 9, requestTelemetry.getId().length());
        assertTrue(requestTelemetry.getId().startsWith(incomingId));
        assertTrue(requestTelemetry.getId().endsWith("_"));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        assertEquals(rootId, operation.getId());
        assertEquals(incomingId, operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedIfNoRequestIdHeader() {

        //setup - no headers
        Hashtable<String, String> headers = new Hashtable<String, String>();
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);

        //validate operation context ID's - there is no parent, so parentId should be null, rootId
        // is newly generated and request.Id is based on new rootId
        OperationContext operation = requestTelemetry.getContext().getOperation();

        assertNotNull(requestTelemetry.getId());
        assertEquals(requestTelemetry.getId(), '|' + operation.getId() + '.');
        assertNull(operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedIfRequestIdEmpty() {

        //setup - empty RequestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, "");

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);

        //validate operation context ID's - there is no parent, so parentId should be null, rootId
        // is newly generated and request.Id is based on new rootId
        OperationContext operation = requestTelemetry.getContext().getOperation();

        assertNotNull(requestTelemetry.getId());
        assertEquals(requestTelemetry.getId(), '|' + operation.getId() + '.');
        assertNull(operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedWithNonHierarchicalRequest() {

        //setup - flat requestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, "guid");

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);

        //validate we have generated proper ID's
        assertNotNull(requestTelemetry.getId());
        assertTrue(requestTelemetry.getId().startsWith("|guid."));
        assertEquals("|guid.".length() + 9, requestTelemetry.getId().length());
        assertTrue(requestTelemetry.getId().endsWith("_"));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        assertEquals("guid", operation.getId());
        assertEquals("guid", operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedWithNonHierarchicalRequest2() {

        //setup - flat requestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, "guid.guid2.guid3");

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);

        //validate we have generated proper ID's
        assertNotNull(requestTelemetry.getId());
        assertTrue(requestTelemetry.getId().startsWith("|guid.guid2.guid3"));
        assertEquals("|guid.guid2.guid3.".length() + 9, requestTelemetry.getId().length());
        assertTrue(requestTelemetry.getId().endsWith("_"));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        assertEquals("guid", operation.getId());
        assertEquals("guid.guid2.guid3", operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedWithNonHierarchicalRequest3() {

        //setup - flat requestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, "guid.guid2_");

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);

        //validate we have generated proper ID's
        assertNotNull(requestTelemetry.getId());
        assertTrue(requestTelemetry.getId().startsWith("|guid.guid2_"));
        assertEquals("|guid.guid2_".length() + 9, requestTelemetry.getId().length());
        assertTrue(requestTelemetry.getId().endsWith("_"));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        assertEquals("guid", operation.getId());
        assertEquals("guid.guid2_", operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedWithNonHierarchicalRequest4() {

        //setup - flat requestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, "guid.");

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);

        //validate we have generated proper ID's
        assertNotNull(requestTelemetry.getId());
        assertTrue(requestTelemetry.getId().startsWith("|guid."));
        assertEquals("|guid.".length() + 9, requestTelemetry.getId().length());
        assertTrue(requestTelemetry.getId().endsWith("_"));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        assertEquals("guid", operation.getId());
        assertEquals("guid.", operation.getParentId());
    }

    @Test
    public void testCorrelationContextPopulated() {

        //setup - empty RequestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String rootId = "9e74f0e5-efc4-41b5-86d1-3524a43bd891";
        String incomingId = "|9e74f0e5-efc4-41b5-86d1-3524a43bd891.bcec871c_1.";

        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers, 1);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);

        //validate
        assertNotNull(requestTelemetry.getId());
        assertEquals(incomingId.length() + 9, requestTelemetry.getId().length());
        assertTrue(requestTelemetry.getId().startsWith(incomingId));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        assertEquals(rootId, operation.getId());
        assertEquals(incomingId, operation.getParentId());

        //validate correlation context has been added as properties
        assertEquals(2, requestTelemetry.getProperties().size());
        assertEquals("value1", requestTelemetry.getProperties().get("key1"));
        assertEquals("value2", requestTelemetry.getProperties().get("key2"));
    }

    @Test
    public void testCorrelationContextPopulatedWithMultipleHeaders() {
        //setup - empty RequestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String rootId = "9e74f0e5-efc4-41b5-86d1-3524a43bd891";
        String incomingId = "|9e74f0e5-efc4-41b5-86d1-3524a43bd891.bcec871c_1.";

        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers, 2);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);

        //validate
        assertNotNull(requestTelemetry.getId());
        assertEquals(incomingId.length() + 9, requestTelemetry.getId().length());
        assertTrue(requestTelemetry.getId().startsWith(incomingId));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        assertEquals(rootId, operation.getId());
        assertEquals(incomingId, operation.getParentId());

        //validate correlation context has been added as properties
        assertEquals(3, requestTelemetry.getProperties().size());
        assertEquals("value1", requestTelemetry.getProperties().get("key1"));
        assertEquals("value2", requestTelemetry.getProperties().get("key2"));
        assertEquals("value3", requestTelemetry.getProperties().get("key3"));
    }

    @Test
    public void testCorrelationContextNotPopulatedIfNoRequestId() {

        //setup - empty RequestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String correlationContext = "key1=value1, key2=value2";
        headers.put(TelemetryCorrelationUtils.CORRELATION_CONTEXT_HEADER_NAME, correlationContext);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);

        //validate no extra properties have been populated
        assertEquals(0, requestTelemetry.getProperties().size());
    }

    @Test
    public void testRequestIdOverflow() {

        //setup - incoming requestId is close to 1024 chars already

        String initialId = "|9e74f0e5-efc4-41b5-86d1-3524a43bd891.";
        String rootId = "9e74f0e5-efc4-41b5-86d1-3524a43bd891";
        String incomingId = initialId;
        String suffix = "bcec871c_1.";
        while (incomingId.length() + suffix.length() < 1024) {
            incomingId += suffix;
        }

        assertTrue(incomingId.length() < 1024);

        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);

        //validate
        assertNotNull(requestTelemetry.getId());
        assertTrue(TelemetryCorrelationUtils.isHierarchicalId(requestTelemetry.getId()));

        //derivedId should be like: "|<rootId>.bcec871c_.#"
        assertEquals(1024, requestTelemetry.getId().length());
        assertTrue(requestTelemetry.getId().startsWith(incomingId.substring(0, 1015)));
        assertTrue(requestTelemetry.getId().endsWith("#"));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        assertEquals(rootId, operation.getId());
        assertEquals(incomingId, operation.getParentId());
    }

    @Test
    public void testRequestIdOverflowWithInvalidRequestId() {

        //setup - incoming requestId is close to 1024 chars already

        String initialId = "9e74f0e5-efc4-41b5-86d1-3524a43bd891";
        String incomingId = initialId;
        String suffix = "bcec871c";
        while (incomingId.length() + suffix.length() < 1024) {
            incomingId += suffix;
        }

        assertTrue(incomingId.length() < 1024);

        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);

        //validate
        assertNotNull(requestTelemetry.getId());
        assertTrue(TelemetryCorrelationUtils.isHierarchicalId(requestTelemetry.getId()));

        //derivedId should be a new one since incoming was not valid: "|guid."
        if (requestTelemetry.getId().length() != 34 && requestTelemetry.getId().length() != 33 ) {
            fail("New id is expected to have 33 or 34 chars.");
        }
        assertTrue(requestTelemetry.getId().startsWith("|"));
        assertTrue(requestTelemetry.getId().endsWith("."));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        assertEquals(incomingId, operation.getId());
        assertEquals(incomingId, operation.getParentId());
    }

    @Test
    public void testChildRequestDependencyIdGeneration() {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingId = "|guid_1.";
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);
        RequestTelemetry requestTelemetry = context.getHttpRequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);
        String childId = TelemetryCorrelationUtils.generateChildDependencyId();

        //validate we have generated proper ID's
        assertNotNull(childId);
        assertEquals(requestTelemetry.getId() + "1.", childId);
    }

    @Test
    public void testChildRequestDependencyIdGenerationWithMultipleRequests() {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingId = "|guid_1.";
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);
        RequestTelemetry requestTelemetry = context.getHttpRequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);
        String childId = TelemetryCorrelationUtils.generateChildDependencyId();

        //validate we have generated proper ID's
        assertNotNull(childId);
        assertEquals(requestTelemetry.getId() + "1.", childId);

        // generate second child
        childId = TelemetryCorrelationUtils.generateChildDependencyId();
        assertNotNull(childId);
        assertEquals(requestTelemetry.getId() + "2.", childId);
    }

    @Test
    public void testChildRequestDependencyIdGenerationWithNonHierarchicalRequestId() {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingId = "guid";
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);
        RequestTelemetry requestTelemetry = context.getHttpRequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);
        String childId = TelemetryCorrelationUtils.generateChildDependencyId();

        //Incoming ID is non-hierarchical, so we must not modidy outgoing (child) id
        assertNotNull(childId);
        assertEquals(incomingId, childId);
    }

    @Test
    public void testChildRequestDependencyIdGenerationWithNoParentId() {

        //setup - make sure no RequestId is set (i.e. no parent)
        Hashtable<String, String> headers = new Hashtable<String, String>();
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        HttpServletResponse response = (HttpServletResponse)ServletUtils.generateDummyServletResponse();

        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);
        RequestTelemetry requestTelemetry = context.getHttpRequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, response, requestTelemetry);
        String childId = TelemetryCorrelationUtils.generateChildDependencyId();

        //validate we have generated proper ID's
        assertNotNull(childId);
        assertEquals(requestTelemetry.getId() + "1.", childId);
    }

    @Test
    public void testRequestContextIsResolved() throws Exception {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingRequestContextHeader = getRequestContextHeaderValue("id1", null);
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, incomingRequestContextHeader);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        when(mockProfileFetcher.fetchApplicationId(anyString(), any(TelemetryConfiguration.class))).thenReturn(new ProfileFetcherResult("id2", ProfileFetcherResultTaskStatus.COMPLETE));

        //run
        TelemetryCorrelationUtils.resolveRequestSource(request, requestTelemetry, "ikey1");

        //validate we have populated the source as expected
        assertEquals(getRequestSourceValue("id1", null), requestTelemetry.getSource());
    }

    @Test
    public void testRequestContextIsNotResolvedWithPendingAppProfileFetch() throws Exception {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingRequestContextHeader = getRequestContextHeaderValue("id1", null);
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, incomingRequestContextHeader);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        when(mockProfileFetcher.fetchApplicationId(anyString(), any(TelemetryConfiguration.class))).thenReturn(new ProfileFetcherResult("id2", ProfileFetcherResultTaskStatus.PENDING));

        //run
        TelemetryCorrelationUtils.resolveRequestSource(request, requestTelemetry, "ikey1");

        //validate source is not set as the task to retrieve appId was still pending.
        assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testRequestContextIsNotResolvedWithFailedAppProfileFetch() throws Exception {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingRequestContextHeader = getRequestContextHeaderValue("id1", null);
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, incomingRequestContextHeader);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        when(mockProfileFetcher.fetchApplicationId(anyString(), any(TelemetryConfiguration.class))).thenReturn(new ProfileFetcherResult("id2", ProfileFetcherResultTaskStatus.FAILED));

        //run
        TelemetryCorrelationUtils.resolveRequestSource(request, requestTelemetry, "ikey1");

        //validate source is not set as the task to retrieve appId failed.
        assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testRequestContextIsNotResolvedForSameApp() throws Exception {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingRequestContextHeader = getRequestContextHeaderValue("id1", null);
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, incomingRequestContextHeader);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //appId returned is the same as incoming one.
        when(mockProfileFetcher.fetchApplicationId(anyString(), any(TelemetryConfiguration.class))).thenReturn(new ProfileFetcherResult("id1", ProfileFetcherResultTaskStatus.COMPLETE));

        //run
        TelemetryCorrelationUtils.resolveRequestSource(request, requestTelemetry, "ikey1");

        //validate source is not set as the incoming request came from the same application.
        assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testRequestContextIsNotResolvedIfNoHeaderPresent() throws Exception {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        when(mockProfileFetcher.fetchApplicationId(anyString(), any(TelemetryConfiguration.class))).thenReturn(new ProfileFetcherResult("id1", ProfileFetcherResultTaskStatus.COMPLETE));

        //run
        TelemetryCorrelationUtils.resolveRequestSource(request, requestTelemetry, "ikey1");

        //validate source is not set as the incoming request had no Request-Context header.
        assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testRequestContextIsNotResolvedIfNoAppIdOrRoleName() throws Exception {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        //Request-Context is available but with no appId or roleName keys
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, "foo=bar");

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        when(mockProfileFetcher.fetchApplicationId(anyString(), any(TelemetryConfiguration.class))).thenReturn(new ProfileFetcherResult("id1", ProfileFetcherResultTaskStatus.COMPLETE));

        //run
        TelemetryCorrelationUtils.resolveRequestSource(request, requestTelemetry, "ikey1");

        //validate source is not set as the incoming request had no appId or roleName.
        assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testRequestContextIsResolvedWithRoleNameOnly() throws Exception {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingRequestContextHeader = getRequestContextHeaderValue(null, "Worker Role");
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, incomingRequestContextHeader);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        when(mockProfileFetcher.fetchApplicationId(anyString(), any(TelemetryConfiguration.class))).thenReturn(new ProfileFetcherResult("id2", ProfileFetcherResultTaskStatus.COMPLETE));

        //run
        TelemetryCorrelationUtils.resolveRequestSource(request, requestTelemetry, "ikey1");

        //validate we have populated the source as expected
        assertEquals(getRequestSourceValue(null, "Worker Role"), requestTelemetry.getSource());
    }

    @Test
    public void testRequestContextIsResolvedWithAppIdAndRoleName() throws Exception {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingRequestContextHeader = getRequestContextHeaderValue("id1", "Worker Role");
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, incomingRequestContextHeader);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        when(mockProfileFetcher.fetchApplicationId(anyString(), any(TelemetryConfiguration.class))).thenReturn(new ProfileFetcherResult("id2", ProfileFetcherResultTaskStatus.COMPLETE));

        //run
        TelemetryCorrelationUtils.resolveRequestSource(request, requestTelemetry, "ikey1");

        //validate we have populated the source as expected
        assertEquals(getRequestSourceValue("id1", "Worker Role"), requestTelemetry.getSource());
    }


    @Test
    public void testRequestContextIsNotResolvedWithNullIkey() throws Exception {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingRequestContextHeader = getRequestContextHeaderValue("id1", "Worker Role");
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, incomingRequestContextHeader);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        when(mockProfileFetcher.fetchApplicationId(anyString(), any(TelemetryConfiguration.class))).thenReturn(new ProfileFetcherResult("id2", ProfileFetcherResultTaskStatus.COMPLETE));

        //run
        TelemetryCorrelationUtils.resolveRequestSource(request, requestTelemetry, null);

        //validate
        assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testRequestContextIsNotResolvedWithNullRequest() throws Exception {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingRequestContextHeader = getRequestContextHeaderValue("id1", "Worker Role");
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, incomingRequestContextHeader);

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        when(mockProfileFetcher.fetchApplicationId(anyString(), any(TelemetryConfiguration.class))).thenReturn(new ProfileFetcherResult("id2", ProfileFetcherResultTaskStatus.COMPLETE));

        //run
        TelemetryCorrelationUtils.resolveRequestSource(null, requestTelemetry, "ike1");

        //validate
        assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testRequestContextIsNotResolvedWithNullRequestTelemetry() throws Exception {

        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingRequestContextHeader = getRequestContextHeaderValue("id1", "Worker Role");
        headers.put(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME, incomingRequestContextHeader);

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        when(mockProfileFetcher.fetchApplicationId(anyString(), any(TelemetryConfiguration.class))).thenReturn(new ProfileFetcherResult("id2", ProfileFetcherResultTaskStatus.COMPLETE));

        //run
        TelemetryCorrelationUtils.resolveRequestSource(request, null, "ike1");

        //validate
        assertNull(requestTelemetry.getSource());
    }

    @Test
    public void testLeadingZerosGuid() {
        assertEquals("bb1141acf7984a6c000076774462e31f", TelemetryCorrelationUtils.uuidToStringNoDashes(UUID.fromString("bb1141ac-f798-4a6c-0000-76774462e31f")));
        assertEquals("000076774462e31fbb1141acf7984a6c", TelemetryCorrelationUtils.uuidToStringNoDashes(UUID.fromString("00007677-4462-e31f-bb11-41acf7984a6c")));
        assertEquals("00000000000000000000000000000000", TelemetryCorrelationUtils.uuidToStringNoDashes(UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    public static String getRequestContextHeaderValue(String appId, String roleName) {

        if (appId == null && roleName == null) {
            return "foo=bar";
        }

        if (roleName == null) {
            return String.format("appId=cid-v1:%s", appId);
        }

        if (appId == null) {
            return String.format("roleName=%s", roleName);
        }

        return String.format("appId=cid-v1:%s, roleName=%s", appId, roleName);
    }

    public static String getRequestSourceValue(String appId, String roleName) {

        if (appId == null && roleName == null) {
            return "someValue";
        }

        if (roleName == null) {
            return String.format("cid-v1:%s", appId);
        }

        if (appId == null) {
            return String.format("roleName:%s", roleName);
        }

        return String.format("cid-v1:%s | roleName:%s", appId, roleName);
    }

}