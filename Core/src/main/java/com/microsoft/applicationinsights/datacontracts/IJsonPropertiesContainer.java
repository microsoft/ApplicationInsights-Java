package com.microsoft.applicationinsights.datacontracts;

import java.io.IOException;

/**
 * Created by gupele on 12/25/2014.
 */
public interface IJsonPropertiesContainer {
    void serialize(StringBuilder stringBuilder) throws IOException;
}
