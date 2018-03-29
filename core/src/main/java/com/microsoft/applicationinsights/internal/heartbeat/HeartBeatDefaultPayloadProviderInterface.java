package com.microsoft.applicationinsights.internal.heartbeat;

import java.util.List;
import java.util.concurrent.Callable;

public interface HeartBeatDefaultPayloadProviderInterface {

  String getName();

  Callable<Boolean> setDefaultPayload(List<String> disableFields, HeartBeatProviderInterface provider);

}
