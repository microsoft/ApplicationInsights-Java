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

package com.microsoft.applicationinsights.web.internal;

import org.junit.Assert;
import org.junit.Test;
import javax.servlet.*;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import com.microsoft.applicationinsights.web.utils.ServletUtils;

import static org.mockito.Mockito.*;

/**
 * Created by yonisha on 2/3/2015.
 */
public class WebRequestTrackingFilterTests {

    @Test
    public void testFilterInitializedSuccessfullyFromConfiguration() throws ServletException {
        Filter filter = createInitializedFilter();
        WebModulesContainer container = ServletUtils.getWebModuleContainer(filter);

        Assert.assertNotNull("Container shouldn't be null", container);
        Assert.assertTrue("Modules container shouldn't be empty", container.getModulesCount() > 0);
    }

    @Test
    public void testFiltersChainWhenExceptionIsThrownOnModulesInvocation() throws Exception {
        Filter filter = createInitializedFilter();

        // mocking
        WebModulesContainer containerMock = ServletUtils.setMockWebModulesContainer(filter);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new Exception("FATAL!");
            }
        }).when(containerMock).invokeOnBeginRequest(any(ServletRequest.class), any(ServletResponse.class));

        FilterChain chain = mock(FilterChain.class);

        ServletRequest request = ServletUtils.generateDummyServletRequest();

        // execute
        filter.doFilter(request, null, chain);

        // validate
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    // region Private methods

    private Filter createInitializedFilter() throws ServletException {
        Filter filter = new WebRequestTrackingFilter();
        filter.init(null);

        return filter;
    }

    // endregion Private methods
}
