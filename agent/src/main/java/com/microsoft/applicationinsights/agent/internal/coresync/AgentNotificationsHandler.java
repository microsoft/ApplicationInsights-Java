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

package com.microsoft.applicationinsights.agent.internal.coresync;

import java.net.URL;
import java.sql.Statement;

/**
 * The Agent will inject code in the user's code that will be activated on predefined methods.
 * Those methods will be directed to the Agent that will need to work with that information by
 * delegating the calls to 'clients', those clients will need to implement the following interface.
 *
 * Created by gupele on 5/6/2015.
 */
public interface AgentNotificationsHandler {
    String getName();

    void onException(String className, String methodName, Throwable throwable);

    void onMethodEnterURL(String name, URL url);

    void onMethodEnterSqlStatement(String name, Statement statement, String sqlStatement);

    void onDefaultMethodEnter(String name);

    void onMethodFinish(String name, Throwable throwable);

    void onMethodFinish(String name);
}
