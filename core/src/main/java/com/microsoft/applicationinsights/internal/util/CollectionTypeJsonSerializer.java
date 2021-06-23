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

import com.azure.monitor.opentelemetry.exporter.implementation.NdJsonSerializer;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.util.List;

public class CollectionTypeJsonSerializer extends SimpleSerializers {

  @Override
  public JsonSerializer<?> findCollectionSerializer(
      SerializationConfig config,
      CollectionType type,
      BeanDescription beanDesc,
      TypeSerializer elementTypeSerializer,
      JsonSerializer<Object> elementValueSerializer) {

    if (isJavaLangObjectListType(type)) {
      return new NdJsonSerializer();
    }
    return super.findCollectionSerializer(
        config, type, beanDesc, elementTypeSerializer, elementValueSerializer);
  }

  private static boolean isJavaLangObjectListType(CollectionType type) {
    JavaType contentType = type.getContentType();
    if (List.class.isAssignableFrom(type.getRawClass())) {
      // this means it's a list
      if (contentType.isJavaLangObject()) {
        return true;
      }
    }
    return false;
  }
}
