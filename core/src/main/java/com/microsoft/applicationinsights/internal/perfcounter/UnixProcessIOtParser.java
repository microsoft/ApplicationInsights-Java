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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gupele on 3/16/2015.
 */
final class UnixProcessIOtParser {

    private static final Logger logger = LoggerFactory.getLogger(UnixProcessIOtParser.class);

    private static final String READ_BYTES_PART = "read_bytes:";
    private static final String WRITE_BYTES_PART = "write_bytes:";

    private UnixParsingState state = new UnixParsingState(2);
    private boolean readBytesDone = false;
    private boolean writeBytesDone = false;

    boolean done() {
        return state.getDoneCounter() == 0;
    }

    double getValue() {
        return state.getReturnValue();
    }

    void process(String line) {
        if (!readBytesDone && parseValue(line, READ_BYTES_PART)) {
            readBytesDone = true;
            return;
        }
        if (!writeBytesDone && parseValue(line, WRITE_BYTES_PART)) {
            writeBytesDone = true;
        }
    }

    private boolean parseValue(String line, String part) {
        int index = line.indexOf(part);
        if (index != -1) {
            String doubleValueAsString = line.substring(index + part.length());
            try {
                state.addToReturnValue(Double.parseDouble(doubleValueAsString.trim()));
                state.decrementDoneCounter();
                return true;
            } catch (Exception e) {
                logger.error("Error in parsing value of UnixProcess counter");
                logger.trace("Error in parsing value of UnixProcess counter", e);
            }
        }

        return false;
    }
}
