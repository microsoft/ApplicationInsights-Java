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

package com.microsoft.applicationinsights.management.rest.client;

import com.microsoft.applicationinsights.management.common.Logger;

/**
 * Created by yonisha on 4/19/2015.
 */
public class RestOperationException extends Exception {

    private static final Logger LOG = Logger.getLogger(RestOperationException.class.toString());
    private OperationExceptionDetails operationExceptionDetails;

    public RestOperationException(String message, OperationExceptionDetails operationExceptionDetails) {
        super(message);

        this.operationExceptionDetails = operationExceptionDetails;

        String exceptionDetails = null;
        if (this.operationExceptionDetails != null) {
            exceptionDetails = this.operationExceptionDetails.getErrorMessage();
        }

        LOG.severe(
                "RestOperationException has been thrown.\nMessage: {0}\nException Details: {1}.",
                this.getMessage(),
                exceptionDetails);
    }

    public OperationExceptionDetails getOperationExceptionDetails() {
        return operationExceptionDetails;
    }
}