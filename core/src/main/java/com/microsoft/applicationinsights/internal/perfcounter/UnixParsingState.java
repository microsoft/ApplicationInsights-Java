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
@SuppressWarnings("deprecation")
public class UnixParsingState {
    // TODO v3 fields private
    /**
     * @deprecated use {@link #getDoneCounter()} and {@link #setDoneCounter(int)}
     */
    @Deprecated
    public int doneCounter;

    /**
     * @deprecated use {@link #getDoneCounter()} and {@link #setDoneCounter(int)}
     */
    @Deprecated
    public double returnValue;

    public UnixParsingState(int doneCounter) {
        this.doneCounter = doneCounter;
    }

    public int getDoneCounter() {
        return doneCounter;
    }

    public void setDoneCounter(int doneCounter) {
        this.doneCounter = doneCounter;
    }

    public void decrementDoneCounter() {
        this.doneCounter--;
    }

    public double getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(double returnValue) {
        this.returnValue = returnValue;
    }

    public void addToReturnValue(double addend) {
        this.returnValue += addend;
    }
}
