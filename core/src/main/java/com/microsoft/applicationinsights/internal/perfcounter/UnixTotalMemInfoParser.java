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
            line.trim();
            String[] strings = line.split(" ");
            parsingData.returnValue += Double.valueOf(strings[strings.length - 2]);
            --(parsingData.doneCounter);
            return true;
        }

        return false;
    }
}
