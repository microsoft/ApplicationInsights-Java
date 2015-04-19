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

package com.microsoft.applicationinsights.internal.perfcounter;

import com.google.common.base.Strings;

/**
 * Created by gupele on 3/16/2015.
 */
final class UnixProcessIOtParser {
    private final static String READ_BYTES_PART = "read_bytes:";
    private final static String WRITE_BYTES_PART = "write_bytes:";

    UnixParsingState state = new UnixParsingState(2);
    boolean readBytesDone = false;
    boolean writeBytesDone = false;

    boolean done() {
        return state.doneCounter == 0;
    }

    double getValue() {
        return state.returnValue;
    }

    void process(String line) {
        if (!readBytesDone) {
            if (parseValue(line, READ_BYTES_PART)) {
                readBytesDone = true;
                return;
            }
        }
        if (!writeBytesDone) {
            if (parseValue(line, WRITE_BYTES_PART)) {
                writeBytesDone = true;
                return;
            }
        }
    }

    private boolean parseValue(String line, String part) {
        int index = line.indexOf(part);
        if (index != -1) {
            String doubleValueAsString = line.substring(index + part.length());
            try {
                state.returnValue += Double.valueOf(doubleValueAsString.trim());
                --(state.doneCounter);
                return true;
            } catch (Exception e) {
            }
        }

        return false;
    }
}
