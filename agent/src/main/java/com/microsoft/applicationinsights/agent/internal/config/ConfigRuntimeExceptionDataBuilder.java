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

import com.microsoft.applicationinsights.agent.internal.common.StringUtils;
import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashSet;

/**
 * The class fetches the data from the Agent's configuration file
 *
 * Created by gupele on 8/17/2016.
 */
final class ConfigRuntimeExceptionDataBuilder {

    private final static int MAX_STACK_SIZE = Integer.MAX_VALUE;

    private final static String FULL_STACK_SIZE_NAME_VALUE = "FULL";

    private final static String NAME_ATTRIBUTE = "name";
    private final static String SUPPRESS_TAG = "Suppress";
    private final static String VALID_TAG = "Valid";
    private final static String MAX_STACK_SIZE_ATTRIBUTE = "stackSize";
    private final static String RUNTIME_EXCEPTION_TAG = "RuntimeException";

    public void setRuntimeExceptionData(Element enclosingTag, AgentBuiltInConfigurationBuilder builtInConfigurationBuilder) {
        DataOfConfigurationForException data = new DataOfConfigurationForException();
        builtInConfigurationBuilder.setDataOfConfigurationForException(data);

        Element rtExceptionElement = fetchMainTag(enclosingTag, data);
        if (rtExceptionElement == null) {
            return;
        }

        FetchStackSize(rtExceptionElement, data);
        FetchSupressedExceptions(rtExceptionElement, data);
        FetchValidPaths(rtExceptionElement, data);
    }

    private Element fetchMainTag(Element enclosingTag, DataOfConfigurationForException data) {
        NodeList nodes = enclosingTag.getElementsByTagName(RUNTIME_EXCEPTION_TAG);
        Element rtExceptionElement = XmlParserUtils.getFirst(nodes);
        boolean enabled = XmlParserUtils.getEnabled(rtExceptionElement, RUNTIME_EXCEPTION_TAG);
        if (!enabled || rtExceptionElement == null) {
            data.setEnabled(false);
            return null;
        }

        data.setEnabled(true);
        return rtExceptionElement;
    }

    private void FetchValidPaths(Element rtExceptionElement, DataOfConfigurationForException data) {
        HashSet<String> suppressedExceptions = fetchSet(rtExceptionElement, VALID_TAG);
        data.setValidPathForExceptions(suppressedExceptions);
    }

    private void FetchSupressedExceptions(Element rtExceptionElement, DataOfConfigurationForException data) {
        HashSet<String> validPaths = fetchSet(rtExceptionElement, SUPPRESS_TAG);
        data.setSuppressedExceptions(validPaths);
    }

    private void FetchStackSize(Element rtExceptionElement, DataOfConfigurationForException data) {
        int stackSize = MAX_STACK_SIZE;

        String maxStackSizeAsString = rtExceptionElement.getAttribute(MAX_STACK_SIZE_ATTRIBUTE);
        if (StringUtils.isNullOrEmpty(maxStackSizeAsString)) {
            data.setStackSize(stackSize);
            return;
        }

        String preparedValue = maxStackSizeAsString.trim().toUpperCase();
        if (!FULL_STACK_SIZE_NAME_VALUE.equals(preparedValue)) {
            try {
                int maxStackSize = Integer.parseInt(preparedValue);
                stackSize = maxStackSize;
            } catch (Exception e) {
                InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Failed to parse attribute %s with value %s, will send full stack", MAX_STACK_SIZE, maxStackSizeAsString);
            }
        }

        data.setStackSize(stackSize);
    }

    private HashSet<String> fetchSet(Element rtExceptionElement, String tagName) {
        HashSet<String> exceptions = new HashSet<String>();
        if (rtExceptionElement != null) {
            NodeList nodes = rtExceptionElement.getElementsByTagName(tagName);

            if (nodes != null && nodes.getLength() > 0) {
                for (int suppressIndex = 0; suppressIndex < nodes.getLength(); ++suppressIndex) {
                    Node suppressNode = nodes.item(suppressIndex);
                    if (suppressNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    Element suppressElement = (Element)suppressNode;

                    String exceptionName = suppressElement.getAttribute(NAME_ATTRIBUTE);
                    if (!StringUtils.isNullOrEmpty(exceptionName)) {
                        exceptions.add(exceptionName);
                    }
                }
            }
        }

        return exceptions;
    }
}
