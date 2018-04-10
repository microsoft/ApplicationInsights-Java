package com.microsoft.applicationinsights.internal.heartbeat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class HeartbeatDefaultPayload {

  private static final List<HeartBeatDefaultPayloadProviderInterface> defaultPayloadProviders =
      new ArrayList<>();

  static {
    defaultPayloadProviders.add(new BaseDefaultHeartbeatPropertyProvider());
    defaultPayloadProviders.add(new WebAppsDefaultHeartbeatProvider());
  }

  public static boolean isDefaultKeyWord(String keyword) {
    for (HeartBeatDefaultPayloadProviderInterface payloadProvider : defaultPayloadProviders) {
      if (payloadProvider.isKeyWord(keyword)) {
        return true;
      }
    }
    return false;
  }

  public static Callable<Boolean> populateDefaultPayload(final List<String> disabledFields, final List<String>
      disabledProviders, final HeartBeatProviderInterface provider) {
    return new Callable<Boolean>() {

      volatile boolean populatedFields = false;
      @Override
      public Boolean call() throws Exception {
        for (HeartBeatDefaultPayloadProviderInterface payloadProvider : defaultPayloadProviders) {
          if (disabledProviders != null && disabledProviders.contains(payloadProvider.getName())) {

            // skip any azure specific modules here
            continue;
          }

          boolean fieldsAreSet = payloadProvider.setDefaultPayload(disabledFields, provider).call();
          populatedFields = populatedFields || fieldsAreSet;
        }
        return populatedFields;
      }
    };
  }

}
