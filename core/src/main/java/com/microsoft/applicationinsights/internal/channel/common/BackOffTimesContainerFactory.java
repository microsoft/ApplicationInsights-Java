/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.internal.channel.common;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

/**
 * The class knows how to create the {@link com.microsoft.applicationinsights.internal.channel.common.BackOffTimesContainer}
 * Based on its name.
 * The name must currently be one of the type names as defined in the ContainerType enum.
 *
 * By default the {@link com.microsoft.applicationinsights.internal.channel.common.ExponentialBackOffTimesContainer} is created.
 *
 * Created by gupele on 2/10/2015.
 */
final class BackOffTimesContainerFactory {
    private enum ContainerType {
        EXPONENTIAL,
        STATIC
    }

    public BackOffTimesContainer create(String typeAsString) {
        ContainerType type = ContainerType.EXPONENTIAL;
        if (Strings.isNullOrEmpty(typeAsString)) {
            InternalLogger.INSTANCE.trace("No back-off container defined, using the default '%s'", type);
        } else {
            try {
                type = ContainerType.valueOf(typeAsString.toUpperCase());
            } catch (Exception e) {
                InternalLogger.INSTANCE.error("Failed to parse '%s', using the default back-off container '%s'", typeAsString, type);
            }
        }

        switch (type) {
            case STATIC:
                return new StaticBackOffTimesContainer();

            default:
                return new ExponentialBackOffTimesContainer();
        }
    }
}
