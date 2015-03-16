package com.microsoft.applicationinsights.internal.perfcounter;

/**
 * Created by gupele on 3/16/2015.
 */
public class UnixParsingState {
    public int doneCounter;
    public double returnValue;

    public UnixParsingState(int doneCounter) {
        this.doneCounter = doneCounter;
    }
}
