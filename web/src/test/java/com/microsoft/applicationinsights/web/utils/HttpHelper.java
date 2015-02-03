package com.microsoft.applicationinsights.web.utils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by yonisha on 2/2/2015.
 */
public class HttpHelper {
        public static void sendGetRequestAndWait(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.4; WOW64; Trident/7.0; Touch; rv:11.0) like Gecko");
        int responseCode = con.getResponseCode();


        try {
            InputStream inputStream = con.getInputStream();
            while (inputStream.read() > 0) {
            }
            inputStream.close();
        }
        catch (Exception exc) {
        }

        try {
            InputStream errorStream = con.getErrorStream();
            while (errorStream .read() > 0) {}
            errorStream .close();
        }
        catch (Exception exc) {
        }

        //wait a little to allow server to complete request
        Thread.sleep(1000);

        System.out.println("Sent GET request to: " + url);
        System.out.println("Response Code: " + responseCode);
    }
}