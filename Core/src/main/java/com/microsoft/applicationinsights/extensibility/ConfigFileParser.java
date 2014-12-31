package com.microsoft.applicationinsights.extensibility;

import java.util.Collection;

/**
 * Created by gupele on 12/31/2014.
 */
interface ConfigFileParser {
    /**
     * This method must be called prior to any use of the instance.
     *
     * Note that currently the method 'swallows' all exceptions and will simply return false on any failure.
     *
     * @param fileName The file to parse
     * @return True on success, false otherwise
     */
    boolean parse(String fileName);

    /**
     * Gets the Text value of an item
     *
     * Note: the value is already returned 'trimmed'
     *
     * @param itemName The item we need to search
     * @return The value of the item, null if not found
     */
    String getTrimmedValue(String itemName);

    /**
     * This method is useful when you need to fetch data from a section where that section
     * has a 'list' of items which have an attribute, all those attributes are returned in a Collection
     * @param sectionName The section to search
     * @param listItemName The item name with in the section
     * @param attributeName The attribute name within the list item
     * @return A collection of data, or empty if not found
     */
    Collection<String> getList(String sectionName, String listItemName, String attributeName);
}
