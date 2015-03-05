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

package com.microsoft.applicationinsights.web.spring;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import com.microsoft.applicationinsights.web.spring.internal.InterceptorRegistry;
import com.microsoft.applicationinsights.web.spring.internal.RequestNameHandlerInterceptorAdapter;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by yonisha on 3/4/2015.
 */
public class InterceptorRegistryTests {

    @Test
    public void testInterceptorRegistryHasRequiredAnnotations() {
        List<Annotation> annotations = Arrays.asList(InterceptorRegistry.class.getAnnotations());

        Assert.assertNotNull(getAnnotationByType(annotations, EnableWebMvc.class));
        Assert.assertNotNull(getAnnotationByType(annotations, Configuration.class));
    }

    @Test
    public void testRegistryAddsRequestNameInterceptor() {
        InterceptorRegistry aiRegistry = new InterceptorRegistry();

        org.springframework.web.servlet.config.annotation.InterceptorRegistry springRegistry =
                mock(org.springframework.web.servlet.config.annotation.InterceptorRegistry.class);
        aiRegistry.addInterceptors(springRegistry);

        verify(springRegistry).addInterceptor(any(RequestNameHandlerInterceptorAdapter.class));
    }

    // region Private

    private <T> Annotation getAnnotationByType(List<Annotation> annotationList, Class<T> tClass) {
        for (Annotation annotation : annotationList) {
            if (tClass.isInstance(annotation)) {
                return annotation;
            }
        }

        return null;
    }

    // region Private
}
