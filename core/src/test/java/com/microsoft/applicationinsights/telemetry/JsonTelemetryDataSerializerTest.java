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

package com.microsoft.applicationinsights.telemetry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;

import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class JsonTelemetryDataSerializerTest {
        private final static class TestClassWithStrings implements JsonSerializable, Serializable {
        private String s1;
        private String s2;

        @Override
        public void serialize(JsonTelemetryDataSerializer serializer) throws IOException {
            serializer.writeRequired("s1", s1, 100);
            serializer.write("s2", s2, 15);
        }

        public String getS1() {
            return s1;
        }

        public void setS1(String s1) {
            this.s1 = s1;
        }

        public String getS2() {
            return s2;
        }

        public void setS2(String s2) {
            this.s2 = s2;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof TestClassWithStrings)) {
                return false;
            }

            TestClassWithStrings that = (TestClassWithStrings)other;
            return this.s1.equals(that.getS1()) && this.s2.equals(that.getS2());
        }
    }

    private final static class StubClass implements JsonSerializable, Serializable {
        private int i1;
        private int i2;
        private String s1;
        private Long l1;
        private long l2;
        private String s2;
        private Map<String, Integer> m1 = new HashMap<String, Integer>();
        private List<String> list1 = new ArrayList<String>();
        private com.microsoft.applicationinsights.internal.schemav2.SeverityLevel severity;
        private boolean b1;
        private short sh1;
        private float f1;
        private Float f2;
        private double d1;
        private Double d2;

        public int getI1() {
            return i1;
        }

        public void setI1(int i1) {
            this.i1 = i1;
        }

        public int getI2() {
            return i2;
        }

        public void setI2(int i2) {
            this.i2 = i2;
        }

        public String getS1() {
            return s1;
        }

        public void setS1(String s1) {
            this.s1 = s1;
        }

        public Long getL1() {
            return l1;
        }

        public void setL1(Long l1) {
            this.l1 = l1;
        }

        public long getL2() {
            return l2;
        }

        public void setL2(long l2) {
            this.l2 = l2;
        }

        public String getS2() {
            return s2;
        }

        public void setS2(String s2) {
            this.s2 = s2;
        }

        public Map<String, Integer> getM1() {
            return m1;
        }

        public void setM1(Map<String, Integer> m1) {
            this.m1 = m1;
        }

        public List<String> getList1() {
            return list1;
        }

        public void setList1(List<String> list1) {
            this.list1 = list1;
        }

        public void setSeverity(com.microsoft.applicationinsights.internal.schemav2.SeverityLevel severity) {
            this.severity = severity;
        }

        public boolean getB1() { return b1; }

        public void setB1(boolean b1) { this.b1 = b1; }

        public short getSh1() { return sh1; }

        public void setSh1(short  sh1) { this.sh1 = sh1; }

        public float getF1() { return f1; }

        public void setF1(float f1) { this.f1 = f1; }

        public Float getF2() { return f2; }

        public void setF2(Float f2) { this.f2 = f2; }

        public double getD1() { return d1; }

        public void setD1(double d1) { this.d1 = d1; }

        public Double getD2() { return d2; }

        public void setD2(Double d2) { this.d2 = d2; }

        @Override
        public void serialize(JsonTelemetryDataSerializer serializer) throws IOException {
            serializer.write("i1", i1);
            serializer.write("i2", i2);
            serializer.writeRequired("s1", s1, 10);
            serializer.write("l1", l1);
            serializer.write("l2", l2);
            serializer.write("s2", s2, 15);
            serializer.write("m1", m1);
            serializer.write("list1", list1);
            serializer.write("severity", severity);
            serializer.write("b1", b1);
            serializer.write("sh1", sh1);
            serializer.write("f1", f1);
            serializer.write("f2", f2);
            serializer.write("d1", d1);
            serializer.write("d2", d2);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof TestClassWithStrings)) {
                return false;
            }

            StubClass that = (StubClass)other;
            return
                    this.s1.equals(that.getS1()) && this.s2.equals(that.getS2()) &&
                    this.i1 == that.getI1() && this.i2 == that.getI2() &&
                            (this.l1 == null ? that.getL1() == null : this.l1.equals(getL1())) && this.l2 == that.getL2() &&
                    this.list1.equals(that.getList1()) && this.m1.equals(that.getM1());
        }
    }

    @Test
    public void testStrings() throws IOException {
        TestClassWithStrings testClassWithStrings = new TestClassWithStrings();
        testClassWithStrings.setS1("s1");
        testClassWithStrings.setS2("s2");

        StringWriter stringWriter = new StringWriter();
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(stringWriter);
        testClassWithStrings.serialize(tested);
        tested.close();
        String str = stringWriter.toString();
        TestClassWithStrings bac = new Gson().fromJson(str, TestClassWithStrings.class);
        assertEquals(bac, testClassWithStrings);
    }

    //This is to test if the write method with name parameters work
    @Test
    public void testLengthOfStrings() throws IOException {
            TestClassWithStrings testClassWithStrings = new TestClassWithStrings();
            String s1 = TelemetryTestsUtils.createString(110);
            testClassWithStrings.setS1(s1);
            testClassWithStrings.setS2("abc");
            StringWriter stringWriter = new StringWriter();
            JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(stringWriter);
            testClassWithStrings.serialize(tested);
            tested.close();
            String str = stringWriter.toString();
            TestClassWithStrings bac = new Gson().fromJson(str, TestClassWithStrings.class);
            Map<String, String> recoveryMap = new Gson().fromJson(str, new TypeToken<HashMap<String, String>>() {}.getType());
            assertNotEquals((recoveryMap.get("s1")).length(), s1.length());
            assertNotEquals(bac, testClassWithStrings);
    }


    @Test
    public void testSanitization() throws IOException {
        TestClassWithStrings testClassWithStrings = new TestClassWithStrings();
        String s1 = "\\'\\f\\b\\f\\n\\r\\t/\\";
        String s2 = "0x0021\t";
        testClassWithStrings.setS1(s1);
        testClassWithStrings.setS2(s2);
        StringWriter stringWriter = new StringWriter();
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(stringWriter);
        testClassWithStrings.serialize(tested);
        tested.close();
        String str = stringWriter.toString();
        Map<String, String> recoveryMap = new Gson().fromJson(str, new TypeToken<HashMap<String, String>>() {}.getType());
        assertEquals(recoveryMap.get("s1"), "\\'\\f\\b\\f\\n\\r\\t/\\");
        assertEquals(recoveryMap.get("s2"), "0x0021\t");
    }

    @Test
    public void testEmptyAndDefaultSanitization() throws IOException {
        TestClassWithStrings testClassWithStrings = new TestClassWithStrings();
        testClassWithStrings.setS1("");
        StringWriter stringWriter = new StringWriter();
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(stringWriter);
        testClassWithStrings.serialize(tested);
        tested.close();
        String str = stringWriter.toString();
        Map<String, String> recoveryMap = new Gson().fromJson(str, new TypeToken<HashMap<String, String>>() {}.getType());
        assertEquals(recoveryMap.get("s1"), "DEFAULT s1");
        assertEquals(recoveryMap.get("s2"), null);
    }

    @Test
    public void testWriteNotRequiredMethodWithEmptyValue() throws IOException {
        TestClassWithStrings testClassWithStrings = new TestClassWithStrings();
        testClassWithStrings.setS2("");
        StringWriter stringWriter = new StringWriter();
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(stringWriter);
        testClassWithStrings.serialize(tested);
        tested.close();
        String str = stringWriter.toString();
        Map<String, String> recoveryMap = new Gson().fromJson(str, new TypeToken<HashMap<String, String>>() {}.getType());
        assertEquals(recoveryMap.get("s1"), "DEFAULT s1");
        assertEquals(recoveryMap.get("s2"), null);
    }

    @Test
    public void test() throws IOException {
        StubClass stubClass = new StubClass();
        stubClass.setI1(1);
        stubClass.setI2(2);
        stubClass.setL1(3L);
        stubClass.setL2(4L);
        stubClass.getList1().add("str1");
        stubClass.getList1().add("str2");
        stubClass.getM1().put("key1", 5);
        stubClass.getM1().put("key2", 6);
        stubClass.getM1().put("key3", 7);
        stubClass.setSeverity(SeverityLevel.Critical);
        stubClass.setB1(true);
        stubClass.setSh1((short)4);
        stubClass.setF1(5.0f);
        stubClass.setF2(6.0f);
        stubClass.setD1(7.0);
        stubClass.setD2(8.0);

        StringWriter stringWriter = new StringWriter();
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(stringWriter);
        stubClass.serialize(tested);
        tested.close();
        String str = stringWriter.toString();

        System.out.println("[test] Serialized StubClass: " + str);

        Gson gson = new Gson();
        StubClass bac = gson.fromJson(str, StubClass.class);
        assertEquals(bac.i1, stubClass.i1);
        assertEquals(bac.i2, stubClass.i2);
        assertEquals(bac.l1, stubClass.l1);
        assertEquals(bac.l2, stubClass.l2);
        assertEquals(bac.list1, stubClass.list1);
        assertEquals(bac.m1, stubClass.m1);
        assertEquals(bac.severity, stubClass.severity);
        assertEquals(bac.b1, stubClass.b1);
        assertEquals(bac.sh1, stubClass.sh1);
        assertEquals(bac.f1, stubClass.f1, 0.001);
        assertEquals(bac.f2, stubClass.f2, 0.001);
        assertEquals(bac.d1, stubClass.d1, 0.001);
        assertEquals(bac.d2, stubClass.d2, 0.001);

        // There's a bug in Gson where it does not respect setLeinient(false) to enable strict parsing/deserialization. There doesn't appear to be a workaround.
        // this should verify that the Json is valid.
        assertEquals(str, gson.toJson(bac));
    }

    @Test
    public void testFloatingPointNaNs() throws IOException {
        StubClass stubClass = new StubClass();
        stubClass.setF1(Float.NaN);
        stubClass.setF2(Float.NaN);
        stubClass.setD1(Double.NaN);
        stubClass.setD2(Double.NaN);

        StringWriter stringWriter = new StringWriter();
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(stringWriter);
        stubClass.serialize(tested);
        tested.close();
        String str = stringWriter.toString();

        System.out.println("[testFloatingPointNaNs] Serialized StubClass: " + str);

        Gson gson = new Gson();
        StubClass bac = gson.fromJson(str, StubClass.class);
        assertEquals(bac.f1, 0, 0.001);
        assertEquals(bac.f2, null);
        assertEquals(bac.d1, 0, 0.001);
        assertEquals(bac.d2, null);
    }

    @Test
    public void testFloatingPointInfinity() throws IOException {
        StubClass stubClass = new StubClass();
        stubClass.setF1(Float.POSITIVE_INFINITY);
        stubClass.setF2(Float.POSITIVE_INFINITY);
        stubClass.setD1(Double.POSITIVE_INFINITY);
        stubClass.setD2(Double.POSITIVE_INFINITY);

        StringWriter stringWriter = new StringWriter();
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(stringWriter);
        stubClass.serialize(tested);
        tested.close();
        String str = stringWriter.toString();

        System.out.println("[testFloatingPointInfinity] Serialized StubClass: " + str);

        Gson gson = new Gson();
        StubClass bac = gson.fromJson(str, StubClass.class);
        assertEquals(bac.f1, 0, 0.001);
        assertEquals(bac.f2, null);
        assertEquals(bac.d1, 0, 0.001);
        assertEquals(bac.d2, null);
    }
}
