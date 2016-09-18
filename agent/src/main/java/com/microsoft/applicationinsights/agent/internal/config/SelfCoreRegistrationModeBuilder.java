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

package com.microsoft.applicationinsights.agent.internal.config;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Created by gupele on 9/11/2016.
 */
public class SelfCoreRegistrationModeBuilder {
    private final static String SELF_MODE_TAG = "SelfMode";
    private final static String SELF_MODE_SDK_PATH_ATTRIBUTE = "sdkPath";

    public void create(AgentConfigurationDefaultImpl agentConfiguration, Element enclosingTag) {
        NodeList nodes = enclosingTag.getElementsByTagName(SELF_MODE_TAG);
        Element selfModeElement = XmlParserUtils.getFirst(nodes);
        if (selfModeElement == null) {
            agentConfiguration.setSelfRegistrationMode(false);
            return;
        }

        boolean enabled = XmlParserUtils.getEnabled(selfModeElement, SELF_MODE_TAG, false);
        agentConfiguration.setSelfRegistrationMode(enabled);

        if (enabled) {
            String sdkPath = XmlParserUtils.getAttribute(selfModeElement, SELF_MODE_SDK_PATH_ATTRIBUTE);
            agentConfiguration.setSdkPath(sdkPath);
        }
    }
}
