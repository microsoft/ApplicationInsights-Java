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

package com.microsoft.applicationinsights.internal.annotation;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import eu.infomas.annotation.AnnotationDetector;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gupele on 3/15/2015.
 */
public final class AnnotationPackageScanner {
    /**
     * The method will scan packages searching for classes that have the needed annotations.
     * @param annotationsToSearch The annotations we need.
     * @param packageToScan The packages to scan from, note that all sub packages will be scanned too.
     * @return A list of class names that are under the package we asked and that carry the needed annotations
     */
    public List<String> scanForClassAnnotations(final Class<? extends Annotation>[] annotationsToSearch, String packageToScan) {
        final ArrayList<String> performanceModuleNames = new ArrayList<String>();
        AnnotationDetector.TypeReporter reporter = new AnnotationDetector.TypeReporter() {
            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends Annotation>[] annotations() {
                return annotationsToSearch;
            }

            @Override
            public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
                performanceModuleNames.add(className);
            }
        };
        final AnnotationDetector annotationDetector = new AnnotationDetector(reporter);
        try {
            annotationDetector.detect(packageToScan);
        } catch (Throwable t) {
            InternalLogger.INSTANCE.error("Failed to scan packages '%s': exception: '%s'", packageToScan, t.toString());
            InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(t));
        }

        return performanceModuleNames;
    }
}
