package com.microsoft.applicationinsights.extensibility;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

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

    /**
     * The method is useful when there is 'structured' data like the following:
     * <SectionName>
     *     <Item1>Value1</Item1>
     *     <Item2>Value2</Item2>
     * </SectionName>
     *
     * The method gets the section name and the item names. It will return a map where the keys
     * are the item names that were found in the section and the values are the text values
     *
     * Note that the the Set of item names are supplied for the method which takes full ownership on it
     * which means it might change its data
     *
     * @param sectionName The tag name that is the root of the structure
     * @param itemNames The tags within the section name that form the needed data
     * @return A map containing the data found, an item that is not found is not present.
     * That means that if nothing is found the method will return an empty map
     */
    Map<String, String> getStructuredData(String sectionName, Set<String> itemNames);
}
