package com.microsoft.applicationinsights.web.internal;

import javax.servlet.*;
import com.microsoft.applicationinsights.web.utils.ServletUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by yonisha on 2/3/2015.
 */
public class WebRequestTrackingFilterTests {

    @Test
    public void testFilterInitializedSuccessfullyFromConfiguration() throws ServletException {
        Filter filter = createInitializedFilter();
        WebModulesContainer container = ServletUtils.getWebModuleContainer(filter);

        Assert.assertNotNull("Something went wrong, container shouldn't be null", container);
        Assert.assertEquals("Exactly one telemetry module should be loaded", 1, container.getModulesCount());
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
