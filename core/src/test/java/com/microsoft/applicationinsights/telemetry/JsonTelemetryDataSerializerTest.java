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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;
import com.squareup.moshi.JsonWriter;
import okio.Buffer;
import org.junit.*;

import static org.junit.Assert.*;

public class JsonTelemetryDataSerializerTest {
        private final static class TestClassWithStrings implements JsonSerializable, Serializable {
        private String s1;
        private String s2;

        @Override
        public void serialize(JsonTelemetryDataSerializer serializer) throws IOException {
            serializer.writeRequired("s1", s1, 100);
            serializer.write("s2", s2, 15);
        }

        public void setS1(String s1) {
            this.s1 = s1;
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
            return this.s1.equals(that.s1) && this.s2.equals(that.s2);
        }
    }

    private final static class StubClass implements JsonSerializable, Serializable {
        private int i1;
        private int i2;
        private Long l1;
        private long l2;
        private Map<String, Integer> m1 = new HashMap<String, Integer>();
        private List<String> list1 = new ArrayList<String>();
        private com.microsoft.applicationinsights.internal.schemav2.SeverityLevel severity;
        private boolean b1;
        private short sh1;
        private float f1;
        private Float f2;
        private double d1;
        private Double d2;

        public void setI1(int i1) {
            this.i1 = i1;
        }

        public void setI2(int i2) {
            this.i2 = i2;
        }

        public void setL1(Long l1) {
            this.l1 = l1;
        }

        public void setL2(long l2) {
            this.l2 = l2;
        }

        public Map<String, Integer> getM1() {
            return m1;
        }

        public List<String> getList1() {
            return list1;
        }

        public void setSeverity(com.microsoft.applicationinsights.internal.schemav2.SeverityLevel severity) {
            this.severity = severity;
        }

        public void setB1(boolean b1) { this.b1 = b1; }

        public void setSh1(short  sh1) { this.sh1 = sh1; }

        public void setF1(float f1) { this.f1 = f1; }

        public void setF2(Float f2) { this.f2 = f2; }

        public void setD1(double d1) { this.d1 = d1; }

        public void setD2(Double d2) { this.d2 = d2; }

        @Override
        public void serialize(JsonTelemetryDataSerializer serializer) throws IOException {
            serializer.write("i1", i1);
            serializer.write("i2", i2);
            serializer.write("l1", l1);
            serializer.write("l2", l2);
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
                    this.i1 == that.i1 && this.i2 == that.i2 &&
                            (this.l1 == null ? that.l1 == null : this.l1.equals(l1)) && this.l2 == that.l2 &&
                    this.list1.equals(that.list1) && this.m1.equals(that.m1);
        }
    }

    @Test
    public void testStrings() throws IOException {
        TestClassWithStrings testClassWithStrings = new TestClassWithStrings();
        testClassWithStrings.setS1("s1");
        testClassWithStrings.setS2("s2");

        Buffer buffer = new Buffer();
        JsonWriter writer = JsonWriter.of(buffer);
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(writer);
        testClassWithStrings.serialize(tested);
        tested.close();
        writer.close();
        String str = new String(buffer.readByteArray(), Charsets.UTF_8);

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
            Buffer buffer = new Buffer();
            JsonWriter writer = JsonWriter.of(buffer);
            JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(writer);
            testClassWithStrings.serialize(tested);
            tested.close();
            writer.close();
            String str = new String(buffer.readByteArray(), Charsets.UTF_8);
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
        Buffer buffer = new Buffer();
        JsonWriter writer = JsonWriter.of(buffer);
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(writer);
        testClassWithStrings.serialize(tested);
        tested.close();
        writer.close();
        String str = new String(buffer.readByteArray(), Charsets.UTF_8);
        Map<String, String> recoveryMap = new Gson().fromJson(str, new TypeToken<HashMap<String, String>>() {}.getType());
        assertEquals("\\'\\f\\b\\f\\n\\r\\t/\\", recoveryMap.get("s1"));
        assertEquals("0x0021\t", recoveryMap.get("s2"));
    }

    @Test
    public void testEmptyAndDefaultSanitization() throws IOException {
        TestClassWithStrings testClassWithStrings = new TestClassWithStrings();
        testClassWithStrings.setS1("");
        Buffer buffer = new Buffer();
        JsonWriter writer = JsonWriter.of(buffer);
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(writer);
        testClassWithStrings.serialize(tested);
        tested.close();
        writer.close();
        String str = new String(buffer.readByteArray(), Charsets.UTF_8);
        Map<String, String> recoveryMap = new Gson().fromJson(str, new TypeToken<HashMap<String, String>>() {}.getType());
        assertEquals("DEFAULT s1", recoveryMap.get("s1"));
        assertEquals(null, recoveryMap.get("s2"));
    }

    @Test
    public void testWriteNotRequiredMethodWithEmptyValue() throws IOException {
        TestClassWithStrings testClassWithStrings = new TestClassWithStrings();
        testClassWithStrings.setS2("");
        Buffer buffer = new Buffer();
        JsonWriter writer = JsonWriter.of(buffer);
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(writer);
        testClassWithStrings.serialize(tested);
        tested.close();
        writer.close();
        String str = new String(buffer.readByteArray(), Charsets.UTF_8);
        Map<String, String> recoveryMap = new Gson().fromJson(str, new TypeToken<HashMap<String, String>>() {}.getType());
        assertEquals("DEFAULT s1", recoveryMap.get("s1"));
        assertNull(recoveryMap.get("s2"));
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

        Buffer buffer = new Buffer();
        JsonWriter writer = JsonWriter.of(buffer);
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(writer);
        stubClass.serialize(tested);
        tested.close();
        writer.close();
        String str = new String(buffer.readByteArray(), Charsets.UTF_8);

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

        Buffer buffer = new Buffer();
        JsonWriter writer = JsonWriter.of(buffer);
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(writer);
        stubClass.serialize(tested);
        tested.close();
        writer.close();
        String str = new String(buffer.readByteArray(), Charsets.UTF_8);

        System.out.println("[testFloatingPointNaNs] Serialized StubClass: " + str);

        Gson gson = new Gson();
        StubClass bac = gson.fromJson(str, StubClass.class);
        final double epsilon = Math.ulp(0.0);
        assertEquals(0, bac.f1, epsilon);
        assertEquals(0, bac.f2, epsilon);
        assertEquals(0, bac.d1, epsilon);
        assertEquals(0, bac.d2, epsilon);
    }

    @Test
    public void testFloatingPointInfinity() throws IOException {
        StubClass stubClass = new StubClass();
        stubClass.setF1(Float.POSITIVE_INFINITY);
        stubClass.setF2(Float.POSITIVE_INFINITY);
        stubClass.setD1(Double.POSITIVE_INFINITY);
        stubClass.setD2(Double.POSITIVE_INFINITY);

        Buffer buffer = new Buffer();
        JsonWriter writer = JsonWriter.of(buffer);
        JsonTelemetryDataSerializer tested = new JsonTelemetryDataSerializer(writer);
        stubClass.serialize(tested);
        tested.close();
        writer.close();
        String str = new String(buffer.readByteArray(), Charsets.UTF_8);

        System.out.println("[testFloatingPointInfinity] Serialized StubClass: " + str);

        Gson gson = new Gson();
        StubClass bac = gson.fromJson(str, StubClass.class);
        final double epsilon = Math.ulp(0.0);
        assertEquals(0, bac.f1, epsilon);
        assertEquals(0, bac.f2, epsilon);
        assertEquals(0, bac.d1, epsilon);
        assertEquals(0, bac.d2, epsilon);
    }
}
