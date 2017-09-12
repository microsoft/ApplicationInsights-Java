package com.microsoft.applicationinsights.common;

import java.text.StringCharacterIterator;

/**
 * Created by dhdoshi on 09/10/2017
 *
 * This class provides utility functions to sanitize strings
 * for various formats. Currently it supports JSON sanitization
 */
public final class SanitizationUtils {

    /**
     * This method appends escape characters to input String to prevent
     * JSON Sanitization failure
     * @param text
     * @return Sanitized String suitable for JSON
     */
    public static String sanitizeStringForJSON(String text) {

        final StringBuilder result = new StringBuilder();
        StringCharacterIterator iterator = new StringCharacterIterator(text);
        for (char curr = iterator.current(); curr != iterator.DONE; curr = iterator.next()) {
            if( curr == '\"' ){
                result.append("\\\"");
            }
            else if (curr == '\'') {
                result.append("\\\'");
            }
            else if(curr == '\\'){
                result.append("\\\\");
            }
            else if(curr == '/'){
                result.append("\\/");
            }
            else if(curr == '\b'){
                result.append("\\b");
            }
            else if(curr == '\f'){
                result.append("\\f");
            }
            else if(curr == '\n'){
                result.append("\\n");
            }
            else if(curr == '\r'){
                result.append("\\r");
            }
            else if(curr == '\t'){
                result.append("\\t");
            }
            else {
                result.append(curr);
            }
        }
        return result.toString();
    }
}
