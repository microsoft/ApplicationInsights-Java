package com.microsoft.applicationinsights.internal.heartbeat;

import java.util.List;
import java.util.concurrent.Callable;

public interface HeartBeatDefaultPayloadProviderInterface {

  String getName();

  boolean isKeyword(String keyword);

  Callable<Boolean> setDefaultPayload(List<String> disableFields, HeartBeatProviderInterface provider);

}
