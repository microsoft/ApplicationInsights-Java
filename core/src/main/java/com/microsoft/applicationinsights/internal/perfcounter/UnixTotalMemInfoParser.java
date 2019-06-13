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
    private static final String MEM_FREE_PREFIX = "MemFree:";
    private static final String BUFFERS_PREFIX = "Buffers";
    private static final String CACHED_PREFIX = "Cached";

    private boolean memFreeDone = false;
    private boolean buffersDone = false;
    private boolean cachedDone = false;
    private UnixParsingState state = new UnixParsingState(3);

    boolean done() {
        return state.getDoneCounter() == 0;
    }

    void process(String line) {
        if (done()) {
            return;
        }

        if (!memFreeDone && parseValue(state, line, MEM_FREE_PREFIX)) {
            memFreeDone = true;
            return;
        }

        if (!buffersDone && parseValue(state, line, BUFFERS_PREFIX)) {
            buffersDone = true;
            return;
        }

        if (!cachedDone && parseValue(state, line, CACHED_PREFIX)) {
            cachedDone = true;
        }
    }

    public double getValue() {
        return state.getReturnValue();
    }

    private boolean parseValue(UnixParsingState parsingData, String line, String part) {
        int index = line.indexOf(part);
        if (index != -1) {
            line = line.trim();
            String[] strings = line.split(" ");
            parsingData.addToReturnValue(Double.parseDouble(strings[strings.length - 2]));
            parsingData.decrementDoneCounter();
            return true;
        }

        return false;
    }
}
