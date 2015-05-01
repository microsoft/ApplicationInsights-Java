package com.microsoft.applicationinsights.azuretest;


/**
 * Created by moralt on 30/4/2015.
 */
public class Helpers {
    public static void sleep(int milliseconds) {
        try {
            System.out.println("Sleeping for " + milliseconds + " milliseconds Zzz...");
            Thread.sleep(milliseconds);
        } catch(InterruptedException ex) {
            System.out.println("Interrupt caught while sleeping.");
        }
    }
}
