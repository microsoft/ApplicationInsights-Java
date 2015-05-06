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

package com.microsoft.applicationinsights.collectd.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yonisha on 5/5/2015.
 *
 * This class represents a plugin and its data source to be excluded.
 */
public class PluginExclusion {
    private String pluginName;
    private List<String> pluginDataSources;

    /**
     * Constructs new @PluginExclusion.
     * @param pluginName The plugin name.
     */
    public PluginExclusion(String pluginName) {
        this(pluginName, new ArrayList<String>());
    }

    /**
     * Constructs new @PluginExclusion.
     * @param pluginName The plugin name.
     * @param pluginDataSources The data source to exclude.
     */
    public PluginExclusion(String pluginName, List<String> pluginDataSources) {
        this.pluginName = pluginName;
        this.pluginDataSources = pluginDataSources;
    }

    /**
     * Gets the plugin name.
     * @return The plugin name.
     */
    public String getPluginName() {
        return this.pluginName;
    }

    /**
     * Gets a value indicating whether the given data source is excluded.
     * @param dataSource The data source to check if excluded.
     * @return True if the data source is excluded, false otherwise.
     */
    public boolean isDataSourceExcluded(String dataSource) {
        // If data source list is empty, then the entire plugin is excluded.
        if (pluginDataSources.isEmpty()) {
            return true;
        }

        for (String source : pluginDataSources) {
            if (source.equalsIgnoreCase(dataSource)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Builds @PluginExclusion from the given configuration string.
     * @param exclusionString The exclusion string from the configuration.
     * @return A new @PluginExclusion object.
     */
    public static PluginExclusion buildPluginExclusion(String exclusionString) {
        Pattern pattern = Pattern.compile("(.*):(.*)");
        Matcher matcher = pattern.matcher(exclusionString);

        PluginExclusion pluginExclusion = null;
        if (matcher.matches()) {
            String pluginName = matcher.group(1);
            String exclude = matcher.group(2);
            List<String> exclusions = Arrays.asList(exclude.split(","));

            pluginExclusion = new PluginExclusion(pluginName, exclusions);
        } else {
            // If no sources provided, we assume that the entire plugin sources should be excluded.
            pluginExclusion = new PluginExclusion(exclusionString);
        }

        return pluginExclusion;
    }
}
