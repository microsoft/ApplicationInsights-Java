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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.http.ParseException;

/**
 * Retrieves the application profile from storage
 */
 public interface AppProfileFetcher extends Closeable {
     /**
      * Fetches the application profile and returns the appId corresponding to the
      * instrumentation key provided.
      * @param instrumentationKey The instrumentation key for which to fetch the appId.
      * @throws ExecutionException
      * @throws InterruptedException
      * @throws IOException
      * @throws ParseException
      */
      ProfileFetcherResult fetchAppProfile(String instrumentationKey) throws InterruptedException, ExecutionException, ParseException, IOException;
 }