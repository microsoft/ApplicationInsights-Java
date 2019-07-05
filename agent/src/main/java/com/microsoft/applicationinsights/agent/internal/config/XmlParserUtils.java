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

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class XmlParserUtils {

    private static final Logger logger = LoggerFactory.getLogger(XmlParserUtils.class);

    private static final String ENABLED_ATTRIBUTE = "enabled";

    public static Element getFirst(NodeList nodes) {
        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }
        return (Element) node;
    }

    public static boolean getEnabled(Element element, String attributeName) {
        if (element == null) {
            return true;
        }
        try {
            String strValue = element.getAttribute(ENABLED_ATTRIBUTE);
            if (!Strings.isNullOrEmpty(strValue)) {
                return Boolean.valueOf(strValue);
            }
            return true;
        } catch (Exception e) {
            logger.error("Failed to parse attribute '{}' of '{}', default value ({}) will be used.", ENABLED_ATTRIBUTE,
                    attributeName, true);
        }
        return true;
    }

    /**
     * Method to get the attribute value for W3C.
     */
    static boolean w3cEnabled(Element element, String attributeName, boolean defaultValue) {
        if (element == null) {
            return defaultValue;
        }
        try {
            String strValue = element.getAttribute(attributeName);
            if (!Strings.isNullOrEmpty(strValue)) {
                return Boolean.valueOf(strValue);
            }
            return defaultValue;
        } catch (Exception e) {
            logger.error("cannot parse the correlation format, will default to AI proprietary correlation", e);
        }
        return defaultValue;
    }

    public static Long getLong(Element element, String elementName) {
        if (element == null) {
            return null;
        }
        try {
            Node node = element.getFirstChild();
            if (node == null) {
                return null;
            }
            String strValue = node.getTextContent();
            if (!Strings.isNullOrEmpty(strValue)) {
                return Long.parseLong(strValue);
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to parse attribute '{}' of '{}'", ENABLED_ATTRIBUTE, elementName);
        }
        return null;
    }
}
