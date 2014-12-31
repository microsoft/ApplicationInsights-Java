package com.microsoft.applicationinsights.extensibility;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The class is responsible for implementing {@link com.microsoft.applicationinsights.extensibility.ConfigFileParser}
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
            ClassLoader classLoader = TelemetryConfigurationFactory.class.getClassLoader();

            InputStream inputStream = classLoader.getResourceAsStream(fileName);
            if (inputStream == null) {
                return false;
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(inputStream);
            doc.getDocumentElement().normalize();

            return true;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
        } catch (Exception e) {
            e.printStackTrace();
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
