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

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;

/**
 * Created by yonisha on 3/11/2015.
 */
public class ServletUtils {

    private static Integer applicationDescriptorDefaultInMinutes;

    // region Public

    /**
     * Gets the request session timeout as configured in the application descriptor (web.xml).
     *
     * @param servletRequest The servlet request.
     * @return The session timeout in seconds or null if no session timeout has been configured.
     */
    public static Integer getRequestSessionTimeout(ServletRequest servletRequest) {
        Integer sessionTimeout = null;

        try {
            sessionTimeout = tryGetSessionTimeoutFromApplicationDescriptor(servletRequest);
        } catch (Exception e) {
        }

        return sessionTimeout;
    }

    // endregion Public

    // region Private

    /**
     * Session timeout extracted from the application descriptor (web.xml) is static and not changed during application
     * lifecycle. Therefore, to reduce I/O and improve performance, we store the session timeout in a static member to
     * be used in subsequent calls.
     * The application descriptor is searched under WEB-INF folder which is commonly used.
     */
    private static Integer tryGetSessionTimeoutFromApplicationDescriptor(ServletRequest servletRequest) throws Exception {
        final String sessionTimeoutXmlPath = "\"web-app/session-config/session-timeout\"";
        final String applicationDescriptorFilePath = "/WEB-INF/web.xml";

        if (applicationDescriptorDefaultInMinutes != null) {
            return applicationDescriptorDefaultInMinutes;
        }

        InputStream resourceAsStream = servletRequest.getServletContext().getResourceAsStream(applicationDescriptorFilePath);
        applicationDescriptorDefaultInMinutes =
                Integer.parseInt(XPathFactory.newInstance().newXPath().
                        compile(sessionTimeoutXmlPath).
                        evaluate(DocumentBuilderFactory.newInstance().newDocumentBuilder().
                                parse(resourceAsStream)));

        Integer applicationDescriptorDefaultInSeconds = applicationDescriptorDefaultInMinutes * 60;
        return applicationDescriptorDefaultInSeconds;
    }

    // endregion Private
}
