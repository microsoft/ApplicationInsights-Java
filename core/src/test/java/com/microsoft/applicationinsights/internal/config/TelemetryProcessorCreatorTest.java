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

package com.microsoft.applicationinsights.internal.config;

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by gupele on 8/7/2016.
 */
public class TelemetryProcessorCreatorTest {

    @Test
    public void testProcessorValidProcessorsWithSetters() {
        TelemetryProcessorXmlElement element = new TelemetryProcessorXmlElement();
        element.setType("com.microsoft.applicationinsights.internal.config.ValidProcessorsWithSetters");

        ParamXmlElement param = new ParamXmlElement();
        param.setName("PropertyA");
        param.setValue("valueA");
        element.getAdds().add(param);

        param = new ParamXmlElement();
        param.setName("PropertyB");
        param.setValue("valueB");
        element.getAdds().add(param);

        TelemetryProcessor result = new TelemetryProcessorCreator().Create(element);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof ValidProcessorsWithSetters);

        ValidProcessorsWithSetters processor = (ValidProcessorsWithSetters)result;
        Assert.assertEquals(processor.propertyA, "valueA");
        Assert.assertEquals(processor.propertyB, "valueB");
    }

    @Test
    public void testProcessorWithoutSetters() {
        TelemetryProcessorXmlElement element = new TelemetryProcessorXmlElement();
        element.setType("com.microsoft.applicationinsights.internal.config.TestProcessorWithoutSetters");

        TelemetryProcessor result = new TelemetryProcessorCreator().Create(element);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof TestProcessorWithoutSetters);
    }

    @Test
    public void testProcessorWithWrongSetterActivatedSetters() {
        TelemetryProcessorXmlElement element = new TelemetryProcessorXmlElement();
        element.setType("com.microsoft.applicationinsights.internal.config.TestProcessorWithoutSetters");
        ParamXmlElement param = new ParamXmlElement();
        param.setName("Property");
        param.setValue("value");
        element.getAdds().add(param);

        TelemetryProcessor result = new TelemetryProcessorCreator().Create(element);

        Assert.assertNull(result);
    }

    @Test
    public void testNullInput() {
        TelemetryProcessor result = new TelemetryProcessorCreator().Create(null);

        Assert.assertNull(result);
    }

    @Test
    public void testBadClass() {
        TelemetryProcessorXmlElement element = new TelemetryProcessorXmlElement();
        element.setType("asdfdsfsffdsadfsafd.afdsafsfdasd");

        TelemetryProcessor result = new TelemetryProcessorCreator().Create(element);

        Assert.assertNull(result);
    }

    @Test
    public void testProcessorThatFailsInCtor() {
        TelemetryProcessorXmlElement element = new TelemetryProcessorXmlElement();
        element.setType("com.microsoft.applicationinsights.internal.config.TestProcessorThatFailsOnCtor");

        TelemetryProcessor result = new TelemetryProcessorCreator().Create(element);

        Assert.assertNull(result);
    }

    @Test
    public void testProcessorThatThrowsInSetter() {
        TelemetryProcessorXmlElement element = new TelemetryProcessorXmlElement();
        element.setType("com.microsoft.applicationinsights.internal.config.TestProcessorThatThrowsOnSetter");
        ParamXmlElement param = new ParamXmlElement();
        param.setName("Property");
        param.setValue("value");
        element.getAdds().add(param);

        TelemetryProcessor result = new TelemetryProcessorCreator().Create(element);

        Assert.assertNull(result);
    }
}
