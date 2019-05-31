package com.microsoft.applicationinsights.agent.internal.model;

class TwoPartCompletion {

    private volatile boolean part1;
    private volatile boolean part2;

    public boolean setPart1() {
        synchronized (this) {
            if (!part1 && part2) {
                part1 = true;
                return true;
            } else {
                part1 = true;
                return false;
            }
        }
    }

    public boolean setPart2() {
        synchronized (this) {
            if (part1 && !part2) {
                part2 = true;
                return true;
            } else {
                part2 = true;
                return false;
            }
        }
    }
}
