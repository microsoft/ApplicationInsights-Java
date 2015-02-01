package com.microsoft.applicationinsights.internal.logger;

import java.io.File;
import java.io.IOException;

/**
 * Created by gupele on 2/1/2015.
 */
public class DefaultLogFileProxyFactory implements LogFileProxyFactory {
    @Override
    public LogFileProxy create(File baseFolder, int maxSizeInMB) throws IOException{
        LogFileProxy result =  new DefaultLogFileProxy(baseFolder, maxSizeInMB);
        return result;
    }
}