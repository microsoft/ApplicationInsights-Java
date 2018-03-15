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

package com.microsoft.applicationinsights.internal.agent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by gupele on 8/6/2015.
 */
final class ArgsFormatter {
    private StringBuilder sb = new StringBuilder();

    String format(Object[] args) {
        try {
            for (Object arg : args) {
                format(arg, ',');
            }
            String argsAsString = sb.deleteCharAt(sb.length() - 1).toString();
            return argsAsString;
        } catch (Exception e) {
        }

        return "";
    }

    void format(Object object, char separator) {
        if (object == null) {
            formatNullObject();
        } else if (object instanceof Collection) {
            formatCollection((Collection)object);
        } else if (object instanceof Map) {
            formatMap((Map) object);
        }  else if (object instanceof Object[]) {
            formatArray((Object[])object);
        } else {
            sb.append(object.toString());
        }
        sb.append(separator);
    }

    private void formatArray(Object[] array) {
        sb.append('[');
        for (Object object : array) {
            format(object, ',');
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(']');
    }

    private void formatCollection(Collection collection) {
        sb.append('[');
        Iterator it = collection.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            format(obj, ',');
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(']');
    }

    private <K,V> void formatMap(Map<K,V> map) {
        sb.append('[');
        for (Map.Entry<K, V> entry : map.entrySet()) {
            format(entry.getKey(), ':');
            format(entry.getValue(), ',');
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(']');
    }

    void formatNullObject() {
        sb.append("null");
    }
}
