// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.fakeingestion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.applicationinsights.smoketest.schemav2.AvailabilityData;
import com.microsoft.applicationinsights.smoketest.schemav2.Base;
import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Domain;
import com.microsoft.applicationinsights.smoketest.schemav2.Duration;
import com.microsoft.applicationinsights.smoketest.schemav2.EventData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.PageViewData;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

class JsonHelper {

  static final Gson GSON =
      new GsonBuilder()
          .serializeNulls()
          .registerTypeHierarchyAdapter(Base.class, new BaseDataContractDeserializer())
          .registerTypeAdapter(TypeToken.get(Duration.class).getType(), new DurationDeserializer())
          .create();

  private static class BaseDataContractDeserializer implements JsonDeserializer<Base> {
    private static final String discriminatorField = "baseType";

    private final Map<String, Class<? extends Domain>> classMap;

    public BaseDataContractDeserializer() {
      classMap = new HashMap<>();
      @SuppressWarnings("unchecked")
      Class<? extends Domain>[] classes =
          new Class[] {
            RequestData.class,
            EventData.class,
            ExceptionData.class,
            MessageData.class,
            MetricData.class,
            RemoteDependencyData.class,
            PageViewData.class,
            AvailabilityData.class
          };

      for (Class<? extends Domain> clazz : classes) {
        classMap.put(clazz.getSimpleName(), clazz);
      }
    }

    @Override
    public Base deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
      JsonObject jo = json.getAsJsonObject();
      String baseType = jo.get(discriminatorField).getAsString();

      Data<Domain> rval = new Data<>();
      JsonObject baseData = jo.get("baseData").getAsJsonObject();
      Class<? extends Domain> domainClass = classMap.get(baseType);
      if (domainClass == null) {
        throw new JsonParseException("Unknown Domain type: " + baseType);
      }
      rval.setBaseType(baseType);
      Domain deserialize = context.deserialize(baseData, TypeToken.get(domainClass).getType());
      rval.setBaseData(deserialize);
      return rval;
    }
  }

  private static class DurationDeserializer implements JsonDeserializer<Duration> {
    @Override
    public Duration deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context) {
      String value = json.getAsString();
      int firstDot = value.indexOf('.');
      boolean hasDays = firstDot > -1 && firstDot < value.indexOf(':');

      String[] parts = value.split("[:.]"); // [days.]hours:minutes:seconds[.milliseconds]

      long[] conversionFactor = new long[] {86400000, 3600000, 60000, 1000, 1};
      int conversionIndex = hasDays ? 0 : 1;
      long duration = 0;
      for (String part : parts) {
        String str = (conversionIndex == conversionFactor.length - 1) ? part.substring(0, 3) : part;
        duration += Long.parseLong(str) * conversionFactor[conversionIndex++];
      }

      return new Duration(duration);
    }
  }

  private JsonHelper() {}
}
