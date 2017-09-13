package com.microsoft.applicationinsights.telemetry;

import java.text.StringCharacterIterator;

/**
 * Created by dhdoshi on 09/10/2017
 *
 * This class provides utility functions to sanitize strings
 * for various formats. Currently it supports JSON sanitization
 */
 final class SanitizationUtils {

    /**
     * This method appends escape characters to input String to prevent
     * JSON Sanitization failure
     * @param text
     * @return Sanitized String suitable for JSON
     */
     static String sanitizeStringForJSON(String text, boolean isKey) {

        final StringBuilder result = new StringBuilder();
        StringCharacterIterator iterator = new StringCharacterIterator(text);

        // allowing delta for the characters to be appended
        int maxAllowedLength = isKey ? 148 : 8190;
        for (char curr = iterator.current(); curr != iterator.DONE && result.length() < maxAllowedLength; curr = iterator.next()) {
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
            else if (!Character.isISOControl(curr)){
                result.append(curr);
            }
            else {
                if (result.length() + 7 < 8192) { // needs 7 more character space to be appended
                    result.append("\\u");
                    result.append((String.format( "%04x", Integer.valueOf(curr))));
                }
                else {
                    break;
                }
            }
        }
        return result.toString();
    }
}
