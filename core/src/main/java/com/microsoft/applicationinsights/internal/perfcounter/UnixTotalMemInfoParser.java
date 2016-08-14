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

/**
 * Created by gupele on 3/16/2015.
 */
final class UnixTotalMemInfoParser {
    private final static String MEM_FREE_PREFIX = "MemFree:";
    private final static String BUFFERS_PREFIX = "Buffers";
    private final static String CACHED_PREFIX = "Cached";

    boolean memFreeDone = false;
    boolean buffersDone = false;
    boolean cachedDone = false;
    UnixParsingState state = new UnixParsingState(3);

    boolean done() {
        return state.doneCounter == 0;
    }

    void process(String line) {
        if (done()) {
            return;
        }

        if (!memFreeDone) {
            if (parseValue(state, line, MEM_FREE_PREFIX)) {
                memFreeDone = true;
                return;
            }
        }
        if (!buffersDone) {
            if (parseValue(state, line, BUFFERS_PREFIX)) {
                buffersDone = true;
                return;
            }
        }
        if (!cachedDone) {
            if (parseValue(state, line, CACHED_PREFIX)) {
                cachedDone = true;
                return;
            }
        }
    }

    public double getValue() {
        return state.returnValue;
    }

    private boolean parseValue(UnixParsingState parsingData, String line, String part) {
        int index = line.indexOf(part);
        if (index != -1) {
            line = line.trim();
            String[] strings = line.split(" ");
            parsingData.returnValue += Double.valueOf(strings[strings.length - 2]);
            --(parsingData.doneCounter);
            return true;
        }

        return false;
    }
}
