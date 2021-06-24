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

package com.microsoft.applicationinsights.internal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class MapUtilTest {

  @Test
  void targetCannotBeNullInCopy() {
    assertThatThrownBy(() -> MapUtil.copy(new HashMap<String, String>(), null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void copyIsNoOpIfSourceIsNullOrEmpty() {
    Map<String, String> source = mock(Map.class);
    Map<String, String> target = mock(Map.class);
    when(source.size()).thenReturn(0);

    MapUtil.copy(source, target);
    // nothing should be put into target
    verify(target, never()).put(anyString(), anyString());
    verify(source, never()).get(any());

    reset(target);

    MapUtil.copy(null, target);
    verify(target, never()).put(anyString(), anyString());
  }

  @Test
  void testCopyIntoHashMap() {
    Map<String, String> source = new HashMap<>();
    Map<String, String> target = new HashMap<>();

    source.put("key1", "value1");
    source.put("key2", null);

    MapUtil.copy(source, target);
    assertThat(target).hasSize(2);
  }

  @Test
  void testCopyIntoConcurrentHashMap() {
    Map<String, String> source = new HashMap<>();
    Map<String, String> target = new ConcurrentHashMap<>();

    source.put("key1", "value1");
    source.put("key2", null);

    MapUtil.copy(source, target);
    assertThat(target).hasSize(1);
  }
}
