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
