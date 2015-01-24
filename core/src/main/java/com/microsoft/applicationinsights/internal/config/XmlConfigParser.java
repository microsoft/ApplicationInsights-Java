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

package com.microsoft.applicationinsights.internal.config;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The class is responsible for implementing {@link ConfigFileParser}
 * as an Xml file
 *
 * Created by gupele on 12/31/2014.
 */
final class XmlConfigParser implements ConfigFileParser {
    private Document doc;

    /**
     * This method must be called prior to any use of the instance.
     *
     * The method will search for the file in the classpath
     * using the class loader and will parse the file assuming it is an Xml file.
     *
     * Note that currently the method 'swallows' all exceptions and will simply return false on any failure.
     *
     * @param fileName The file to parse
     * @return True on success, false otherwise
     */
    @Override
    public boolean parse(String fileName) {
        try {
            InputStream inputStream = getConfigurationAsInputStream(fileName);
            if (inputStream == null) {
                InternalLogger.INSTANCE.log("Could not find configuration file: %s.", fileName);

                return false;
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(inputStream);
            doc.getDocumentElement().normalize();

            return true;
        } catch (ParserConfigurationException e) {
            InternalLogger.INSTANCE.log("Failed to parse file %s, exception: %s", fileName, e.getMessage());
        } catch (SAXException e) {
            InternalLogger.INSTANCE.log("Failed to parse file %s, exception: %s", fileName, e.getMessage());
        } catch (IOException e) {
            InternalLogger.INSTANCE.log("Failed to parse file %s, exception: %s", fileName, e.getMessage());
        } catch (Exception e) {
            InternalLogger.INSTANCE.log("Failed to parse file %s, exception: %s", fileName, e.getMessage());
        }

        return false;
    }

    /**
     * Gets the Text value of an item
     *
     * Note: the value is already returned 'trimmed'
     *
     * @param itemName The item we need to search
     * @return The value of the item, null if not found
     */
    @Override
    public String getTrimmedValue(String itemName) {
        Element element = getFirstElementInDoc(itemName);
        if (element != null) {
            return element.getTextContent().trim();
        }

        return null;
    }

    /**
     * This method is useful when you need to fetch data from a section where that section
     * has a 'list' of items which have an attribute, all those attributes are returned in a Collection
     * @param sectionName The section to search
     * @param listItemName The item name with in the section
     * @param attributeName The attribute name within the list item
     * @return A collection of data, or empty if not found
     */
    @Override
    public Collection<String> getList(String sectionName, String listItemName, String attributeName) {
        List<String> result = new ArrayList<String>();

        Element sectionElement = getFirstElementInDoc(sectionName);
        if (sectionElement == null) {
            return result;
        }

        NodeList items = sectionElement.getElementsByTagName(listItemName);

        // Find all elements in the list and add them
        for (int i = 0; i < items.getLength(); ++i) {
            Node elementItem = items.item(i);
            if (elementItem.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String attributeValue = ((Element)elementItem).getAttribute(attributeName);
            if (attributeValue != null) {
                result.add(attributeValue);
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructuredDataResult getStructuredData(String sectionName, String sectionTag) {
        HashMap<String, String> items = new HashMap<String, String>();

        String sectionTagValue = null;
        Element sectionElement = getFirstElementInDoc(sectionName);
        if (sectionElement == null) {
            return new StructuredDataResult(sectionTagValue, items);
        }

        if (!Strings.isNullOrEmpty(sectionTag)) {
            sectionTagValue = sectionElement.getAttribute(sectionTag);
        }

        NodeList nodes = sectionElement.getChildNodes();
        for (int i = 0; i < nodes.getLength(); ++i) {
            Node elementItem = nodes.item(i);
            if (elementItem.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String elementName = elementItem.getNodeName().trim();
            items.put(elementName, elementItem.getTextContent().trim());
        }

        return new StructuredDataResult(sectionTagValue, items);
    }


    /**
     * Gets the configuration as input stream.
     * @param fileName Configuration file name.
     * @return The configuration file as input stream.
     */
    private InputStream getConfigurationAsInputStream(String fileName) {

        // Trying to load configuration as a resource.
        ClassLoader classLoader = TelemetryConfigurationFactory.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // If not found as a resource, trying to load from the executing jar directory
        if (inputStream == null) {
            try {
                String jarFullPath = TelemetryConfigurationFactory.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                File jarFile = new File(jarFullPath);

                if (jarFile.exists()) {
                    String jarDirectory = jarFile.getParent();
                    String configurationPath = jarDirectory + "/" + fileName;

                    inputStream = new FileInputStream(configurationPath);
                }
            } catch (URISyntaxException e) {
            } catch (FileNotFoundException e) {
            }
        }

        return inputStream;
    }

    /**
     * An helper method that will return the first Element in the document by the section name
     *
     * This method is useful when you expect to have a section name that appears only once in the file
     *
     * @param sectionName The name to search
     * @return The Element we found, null otherwise
     */
    private Element getFirstElementInDoc(String sectionName) {
        NodeList nodeList = doc.getElementsByTagName(sectionName);
        Element sectionElement = getFirstElement(nodeList);

        return sectionElement;
    }

    /**
     * A common, static, helper method that will return the first Element in a NodeList
     *
     * @param nodeList The node list to work with
     * @return The Element we found, null otherwise
     */
    private static Element getFirstElement(NodeList nodeList) {
        for (int counter = 0; counter < nodeList.getLength(); ++counter) {
            Node elementNode = nodeList.item(counter);
            if (elementNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            return (Element) elementNode;
        }

        return null;
    }
}
