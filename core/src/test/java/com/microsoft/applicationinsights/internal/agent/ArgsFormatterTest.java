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

package com.microsoft.applicationinsights.internal.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.junit.Test;

public final class ArgsFormatterTest {
  @Test
  public void arrayTest() {
    String s =
        new ArgsFormatter()
            .format(new Object[] {null, new Integer[] {new Integer(1), null, new Integer(122)}});

    assertNotNull(s);
    assertEquals(s, "null,[1,null,122]");
  }

  @Test
  public void nullObjectAndObjectTest() {
    String s = new ArgsFormatter().format(new Object[] {null, new Integer(1)});

    assertNotNull(s);
    assertEquals(s, "null,1");
  }

  @Test
  public void nullObjectTest() {
    String s = new ArgsFormatter().format(new Object[] {null});

    assertNotNull(s);
    assertEquals(s, "null");
  }

  @Test
  public void twoNullsObjectTest() {
    String s = new ArgsFormatter().format(new Object[] {null, null});

    assertNotNull(s);
    assertEquals(s, "null,null");
  }

  @Test
  public void twoObjectsTest() {
    String s1 = "tutorial-list";
    String s2 = "Mysql";
    String s = new ArgsFormatter().format(new Object[] {s1, s2});

    assertNotNull(s);
    assertEquals(s, "tutorial-list,Mysql");
  }

  @Test
  public void oneObjectTest() {
    String s1 = "1";
    String s = new ArgsFormatter().format(new Object[] {s1});

    assertNotNull(s);
    assertEquals(s, "1");
  }

  @Test
  public void twoObjectsAndCollectionTest() {
    String s1 = "1";
    String s2 = "2";
    ArrayList<String> strings = new ArrayList<String>();
    strings.add("a");
    strings.add("a1");
    String s = new ArgsFormatter().format(new Object[] {s1, s2, strings});

    assertNotNull(s);
    assertEquals(s, "1,2,[a,a1]");
  }

  @Test
  public void collectionAndTwoObjectsTest() {
    String s1 = "1";
    String s2 = "2";
    ArrayList<String> strings = new ArrayList<String>();
    strings.add("a");
    strings.add("a1");
    String s = new ArgsFormatter().format(new Object[] {strings, s1, s2});

    assertNotNull(s);
    assertEquals(s, "[a,a1],1,2");
  }

  @Test
  public void twoObjectsAndMapTest() {
    String s1 = "1";
    String s2 = "2";
    TreeMap<String, String> strings = new TreeMap<String, String>();
    strings.put("a", "object");
    strings.put("a1", "object1");
    String s = new ArgsFormatter().format(new Object[] {s1, s2, strings});

    assertNotNull(s);
    assertEquals(s, "1,2,[a:object,a1:object1]");
  }

  @Test
  public void mapOfCollectionsTest() {
    TreeMap<String, List<String>> mapOfStrings = new TreeMap<String, List<String>>();
    ArrayList<String> strings = new ArrayList<String>();
    strings.add("a");
    strings.add("a1");
    mapOfStrings.put("a", strings);
    strings = new ArrayList<String>();
    strings.add("a_");
    strings.add("a_1");
    mapOfStrings.put("a1", strings);
    String s = new ArgsFormatter().format(new Object[] {mapOfStrings});

    assertNotNull(s);
    assertEquals(s, "[a:[a,a1],a1:[a_,a_1]]");
  }

  @Test
  public void collectionTest() {
    ArrayList<String> strings = new ArrayList<String>();
    strings.add("a");
    strings.add("a1");
    String s = new ArgsFormatter().format(new Object[] {strings});

    assertNotNull(s);
    assertEquals(s, "[a,a1]");
  }

  @Test
  public void mapTest() {
    TreeMap<String, String> strings = new TreeMap<String, String>();
    strings.put("a", "object");
    strings.put("a1", "object1");
    String s = new ArgsFormatter().format(new Object[] {strings});

    assertNotNull(s);
    assertEquals(s, "[a:object,a1:object1]");
  }
}
