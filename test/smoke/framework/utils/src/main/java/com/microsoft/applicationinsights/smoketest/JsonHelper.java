package com.microsoft.applicationinsights.smoketest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.microsoft.applicationinsights.internal.schemav2.Base;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;
import com.microsoft.applicationinsights.internal.schemav2.PageViewData;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.telemetry.Duration;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class JsonHelper {
    public static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .registerTypeHierarchyAdapter(Base.class, new BaseDataContractDeserializer())
            .registerTypeAdapter(TypeToken.get(Duration.class).getType(), new DurationDeserializer())
            .registerTypeAdapter(TypeToken.get(DataPointType.class).getType(), new DataPointTypeEnumConverter())
            .create();

    private static class BaseDataContractDeserializer implements JsonDeserializer<Base> {
        private static final String discriminatorField = "baseType";

        private final Map<String, Class<? extends Domain>> classMap;

        public BaseDataContractDeserializer() {
            classMap = new HashMap<>();
            Class<? extends Domain>[] classes = new Class[] {
                RequestData.class,
                EventData.class,
                ExceptionData.class,
                MessageData.class,
                MetricData.class,
                RemoteDependencyData.class,
                PageViewData.class
            };

            for (Class<? extends Domain> clazz : classes) {
                classMap.put(clazz.getSimpleName(), clazz);
            }
        }

        @Override
        public Base deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jo = json.getAsJsonObject();
            String baseType = jo.get(discriminatorField).getAsString();
            try {
                Data<Domain> rval = Data.class.newInstance();
                JsonObject baseData = jo.get("baseData").getAsJsonObject();
                Class<? extends Domain> domainClass = classMap.get(baseType);
                if (domainClass == null) {
                    throw new JsonParseException("Unknown Domain type: "+baseType);
                }
                rval.setBaseType(baseType);
                final Domain deserialize = context.deserialize(baseData, TypeToken.get(domainClass).getType());
                rval.setBaseData(deserialize);
                return rval;
            } catch (InstantiationException | IllegalAccessException e) {
                System.err.println("Error deserializing data");
                e.printStackTrace();
                throw new JsonParseException(e);
            }
        }

    }

    private static class DurationDeserializer implements JsonDeserializer<Duration> {
        @Override
        public Duration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            final String value = json.getAsString();
            final int firstDot = value.indexOf('.');
            final boolean hasDays = firstDot > -1 && firstDot < value.indexOf(':');

            final String[] parts = value.split("[:.]"); // [days.]hours:minutes:seconds[.milliseconds]

            final long[] conversionFactor = new long[]{86400000, 3600000, 60000, 1000, 1};
            int conversionIndex = hasDays ? 0 : 1;
            long duration = 0;
            for (int i = 0; i < parts.length; i++) {
                String part = (conversionIndex == conversionFactor.length-1) ? parts[i].substring(0,3) : parts[i];
                duration += Long.parseLong(part) * conversionFactor[conversionIndex++];
            }

            return new Duration(duration);
        }
    }

    private static class DataPointTypeEnumConverter implements JsonDeserializer<DataPointType>, JsonSerializer<DataPointType> {
        @Override
        public DataPointType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final String id = json.getAsString();
            switch (id) {
                // FIXME (trask) breeze question: this used to be mapped from int (0/1), is it really correct to map to string now?
                case "Measurement": return DataPointType.Measurement;
                case "Aggregation": return DataPointType.Aggregation;
                default: throw new IllegalArgumentException("No DataPointType with id="+id);
            }
        }
        @Override
        public JsonElement serialize(DataPointType src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getValue());
        }
    }
}