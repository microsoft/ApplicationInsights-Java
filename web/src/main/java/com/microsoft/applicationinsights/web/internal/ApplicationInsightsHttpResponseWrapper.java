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

package com.microsoft.applicationinsights.web.internal;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * This class wraps the HTTP servlet response in order to store the response status code.
 * This wrapper is used to support servlet API 2.5, which lacks the api to get response status.
 * Created by yonisha on 5/27/2015.
 */
@Deprecated
public class ApplicationInsightsHttpResponseWrapper extends HttpServletResponseWrapper {

    private int httpStatusCode = SC_OK;

    public ApplicationInsightsHttpResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void setStatus(int sc) {
        super.setStatus(sc);

        this.httpStatusCode = sc;
    }

    @Override
    public void setStatus(int sc, String sm) {
        super.setStatus(sc, sm);

        this.httpStatusCode = sc;
    }

    @Override
    public void sendError(int sc) throws IOException {
        super.sendError(sc);

        this.httpStatusCode = sc;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        super.sendError(sc, msg);

        this.httpStatusCode = sc;
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        super.sendRedirect(location);

        this.httpStatusCode = SC_MOVED_TEMPORARILY;
    }

    @Override
    public void reset() {
        super.reset();
        this.httpStatusCode = SC_OK;
    }

    /**
     * Gets the response status.
     * @return The response status.
     */
    public int getStatus() {
        return httpStatusCode;
    }
}
