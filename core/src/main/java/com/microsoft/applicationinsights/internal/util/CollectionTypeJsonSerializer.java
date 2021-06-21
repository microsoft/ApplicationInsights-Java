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
    public JsonSerializer<?> findCollectionSerializer(SerializationConfig config,
                                                      CollectionType type,
                                                      BeanDescription beanDesc,
                                                      TypeSerializer elementTypeSerializer,
                                                      JsonSerializer<Object> elementValueSerializer) {

        if (isJavaLangObjectListType(type)) {
            return new NdJsonSerializer();
        }
        return super.findCollectionSerializer(config, type, beanDesc, elementTypeSerializer, elementValueSerializer);
    }


    private static boolean isJavaLangObjectListType(CollectionType type) {
        JavaType contentType = type.getContentType();
        if (List.class.isAssignableFrom(type.getRawClass())) {
            // this means it's a list
            if(contentType.isJavaLangObject()) {
                return true;
            }
        }
        return false;
    }
}
