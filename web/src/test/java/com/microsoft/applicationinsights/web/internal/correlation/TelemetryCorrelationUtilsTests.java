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

import org.junit.Assert;
import org.junit.Test;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.utils.ServletUtils;
import java.util.Hashtable;
import javax.servlet.http.HttpServletRequest;


public class TelemetryCorrelationUtilsTests {
    
    @Test
    public void testIsHierarchicalIdValidCase() {

        //setup
        String id = "|9e74f0e5-efc4-41b5-86d1-3524a43bd891.bcec871c_1.";

        //validate
        Assert.assertTrue(TelemetryCorrelationUtils.isHierarchicalId(id));
    }

    @Test
    public void testIsHierarchicalIdEmpty() {

        //setup
        String id = "";

        //validate
        Assert.assertFalse(TelemetryCorrelationUtils.isHierarchicalId(id));
    }

    @Test
    public void testIsHierarchicalIdNull() {

        //setup
        String id = null;

        //validate
        Assert.assertFalse(TelemetryCorrelationUtils.isHierarchicalId(id));
    }

    @Test
    public void testIsHierarchicalIdInvalidCase() {

        //setup
        String id = "9e74f0e5-efc4-41b5-86d1-3524a43bd891.bcec871c_1.";

        //validate
        Assert.assertFalse(TelemetryCorrelationUtils.isHierarchicalId(id));
    }

    @Test
    public void testCorrelationIdsAreResolved() {
        
        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String rootId = "9e74f0e5-efc4-41b5-86d1-3524a43bd891";
        String incomingId = "|9e74f0e5-efc4-41b5-86d1-3524a43bd891.bcec871c_1.";
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);
        
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, requestTelemetry);

        //validate we have generated proper ID's
        Assert.assertNotNull(requestTelemetry.getId());
        Assert.assertEquals(incomingId.length() + 9, requestTelemetry.getId().length());
        Assert.assertTrue(requestTelemetry.getId().startsWith(incomingId));
        Assert.assertTrue(requestTelemetry.getId().endsWith("_"));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        Assert.assertEquals(rootId, operation.getId());
        Assert.assertEquals(incomingId, operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedIfNoRequestIdHeader() {
        
        //setup - no headers
        Hashtable<String, String> headers = new Hashtable<String, String>();
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, requestTelemetry);

        //validate operation context ID's - there is no parent, so parentId should be null, rootId
        // is newly generated and request.Id is based on new rootId
        OperationContext operation = requestTelemetry.getContext().getOperation();
        
        Assert.assertNotNull(requestTelemetry.getId());
        Assert.assertEquals(requestTelemetry.getId(), '|' + operation.getId() + '.');
        Assert.assertNull(operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedIfRequestIdEmpty() {
        
        //setup - empty RequestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, "");

        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, requestTelemetry);

        //validate operation context ID's - there is no parent, so parentId should be null, rootId
        // is newly generated and request.Id is based on new rootId
        OperationContext operation = requestTelemetry.getContext().getOperation();
        
        Assert.assertNotNull(requestTelemetry.getId());
        Assert.assertEquals(requestTelemetry.getId(), '|' + operation.getId() + '.');
        Assert.assertNull(operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedWithNonHierarchicalRequest() {
        
        //setup - flat requestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, "guid");
        
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, requestTelemetry);

        //validate we have generated proper ID's 
        Assert.assertNotNull(requestTelemetry.getId());
        Assert.assertTrue(requestTelemetry.getId().startsWith("|guid."));
        Assert.assertEquals("|guid.".length() + 9, requestTelemetry.getId().length());
        Assert.assertTrue(requestTelemetry.getId().endsWith("_"));
        
        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        Assert.assertEquals("guid", operation.getId());
        Assert.assertEquals("guid", operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedWithNonHierarchicalRequest2() {
        
        //setup - flat requestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, "guid.guid2.guid3");
        
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, requestTelemetry);

        //validate we have generated proper ID's 
        Assert.assertNotNull(requestTelemetry.getId());
        Assert.assertTrue(requestTelemetry.getId().startsWith("|guid.guid2.guid3"));
        Assert.assertEquals("|guid.guid2.guid3.".length() + 9, requestTelemetry.getId().length());
        Assert.assertTrue(requestTelemetry.getId().endsWith("_"));
        
        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        Assert.assertEquals("guid", operation.getId());
        Assert.assertEquals("guid.guid2.guid3", operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedWithNonHierarchicalRequest3() {
        
        //setup - flat requestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, "guid.guid2_");
        
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, requestTelemetry);

        //validate we have generated proper ID's 
        Assert.assertNotNull(requestTelemetry.getId());
        Assert.assertTrue(requestTelemetry.getId().startsWith("|guid.guid2_"));
        Assert.assertEquals("|guid.guid2_".length() + 9, requestTelemetry.getId().length());
        Assert.assertTrue(requestTelemetry.getId().endsWith("_"));
        
        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        Assert.assertEquals("guid", operation.getId());
        Assert.assertEquals("guid.guid2_", operation.getParentId());
    }

    @Test
    public void testCorrelationIdsAreResolvedWithNonHierarchicalRequest4() {
        
        //setup - flat requestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, "guid.");
        
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, requestTelemetry);

        //validate we have generated proper ID's 
        Assert.assertNotNull(requestTelemetry.getId());
        Assert.assertTrue(requestTelemetry.getId().startsWith("|guid."));
        Assert.assertEquals("|guid.".length() + 9, requestTelemetry.getId().length());
        Assert.assertTrue(requestTelemetry.getId().endsWith("_"));
        
        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        Assert.assertEquals("guid", operation.getId());
        Assert.assertEquals("guid.", operation.getParentId());
    }

    @Test
    public void testCorrelationContextPopulated() {
        
        //setup - empty RequestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String rootId = "9e74f0e5-efc4-41b5-86d1-3524a43bd891";
        String incomingId = "|9e74f0e5-efc4-41b5-86d1-3524a43bd891.bcec871c_1.";
        String correlationContext = "key1=value1, key2=value2";
        
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);
        headers.put(TelemetryCorrelationUtils.CORRELATION_CONTEXT_HEADER_NAME, correlationContext);
        
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, requestTelemetry);
        
        //validate 
        Assert.assertNotNull(requestTelemetry.getId());
        Assert.assertEquals(incomingId.length() + 9, requestTelemetry.getId());
        Assert.assertTrue(requestTelemetry.getId().startsWith(incomingId));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        Assert.assertEquals(rootId, operation.getId());
        Assert.assertEquals(incomingId, operation.getParentId());

        //validate correlation context has been added as properties
        Assert.assertEquals(2, requestTelemetry.getProperties().size());
        Assert.assertEquals("value1", requestTelemetry.getProperties().get("key1"));
        Assert.assertEquals("value2", requestTelemetry.getProperties().get("key2"));
    }

    @Test
    public void testCorrelationContextNotPopulatedIfNoRequestId() {
        
        //setup - empty RequestId
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String correlationContext = "key1=value1, key2=value2";
        headers.put(TelemetryCorrelationUtils.CORRELATION_CONTEXT_HEADER_NAME, correlationContext);
        
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, requestTelemetry);

        //validate no extra properties have been populated
        Assert.assertEquals(0, requestTelemetry.getProperties().size());
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

        Assert.assertTrue(incomingId.length() < 1024);

        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);
        
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, requestTelemetry);

        //validate 
        Assert.assertNotNull(requestTelemetry.getId());
        Assert.assertTrue(TelemetryCorrelationUtils.isHierarchicalId(requestTelemetry.getId()));
        
        //derivedId should be like: "|<rootId>.bcec871c_.#"
        Assert.assertEquals(1024, requestTelemetry.getId().length());
        Assert.assertTrue(requestTelemetry.getId().startsWith(incomingId.substring(0, 1015)));
        Assert.assertTrue(requestTelemetry.getId().endsWith("#"));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        Assert.assertEquals(rootId, operation.getId());
        Assert.assertEquals(incomingId, operation.getParentId());
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

        Assert.assertTrue(incomingId.length() < 1024);

        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);
        
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, requestTelemetry);

        //validate 
        Assert.assertNotNull(requestTelemetry.getId());
        Assert.assertTrue(TelemetryCorrelationUtils.isHierarchicalId(requestTelemetry.getId()));
        
        //derivedId should be a new one since incoming was not valid: "|guid."
        Assert.assertEquals(34, requestTelemetry.getId().length());
        Assert.assertTrue(requestTelemetry.getId().startsWith("|"));
        Assert.assertTrue(requestTelemetry.getId().endsWith("."));

        //validate operation context ID's
        OperationContext operation = requestTelemetry.getContext().getOperation();
        Assert.assertEquals(incomingId, operation.getId());
        Assert.assertEquals(incomingId, operation.getParentId());
    }

    @Test
    public void testChildRequestDependencyIdGeneration() {
        
        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingId = "|guid_1.";
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);
        
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, requestTelemetry);
        String childId = TelemetryCorrelationUtils.generateChildDependencyId(requestTelemetry);

        //validate we have generated proper ID's
        Assert.assertNotNull(childId);
        Assert.assertEquals(requestTelemetry.getId().length() + 2, childId.length());
        Assert.assertEquals(requestTelemetry.getId() + "1.", childId);
        Assert.assertTrue(childId.endsWith("."));
    }

    @Test
    public void testChildRequestDependencyIdGenerationWithMultipleRequests() {
        
        //setup
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String incomingId = "|guid_1.";
        headers.put(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME, incomingId);
        
        HttpServletRequest request = ServletUtils.createServletRequestWithHeaders(headers);
        
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        //run
        TelemetryCorrelationUtils.resolveCorrelation(request, requestTelemetry);
        String childId = TelemetryCorrelationUtils.generateChildDependencyId(requestTelemetry);

        //validate we have generated proper ID's
        Assert.assertNotNull(childId);
        Assert.assertEquals(requestTelemetry.getId().length() + 2, childId.length());
        Assert.assertEquals(requestTelemetry.getId() + "1.", childId);
        Assert.assertTrue(childId.endsWith("."));

        // generate second "request"
        childId = TelemetryCorrelationUtils.generateChildDependencyId(requestTelemetry);
        Assert.assertNotNull(childId);
        Assert.assertEquals(requestTelemetry.getId().length() + 2, childId.length());
        Assert.assertEquals(requestTelemetry.getId() + "2.", childId);
        Assert.assertTrue(childId.endsWith("."));

    }


}