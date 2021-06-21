package com.microsoft.applicationinsights.internal.heartbeat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Container for storing default payload and it's associated helpers.
 *
 * @author Dhaval Doshi
 */
public class HeartbeatDefaultPayload {

  /**
   * List of default payloads which would be added
   */
  private static final List<HeartBeatPayloadProviderInterface> defaultPayloadProviders = new ArrayList<>();

  static {
    defaultPayloadProviders.add(new DefaultHeartBeatPropertyProvider());
    defaultPayloadProviders.add(new WebAppsHeartbeatProvider());
  }

  /**
   * Callable which delegates calls to providers for adding payload.
   * @param disabledFields the properties which are disabled by user
   * @param disabledProviders providers which are disabled by users
   * @param provider The HeartBeat provider
   * @return Callable to perform execution
   */
  public static Callable<Boolean> populateDefaultPayload(List<String> disabledFields, List<String>
      disabledProviders, HeartBeatProviderInterface provider) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        boolean populatedFields = false;
        for (HeartBeatPayloadProviderInterface payloadProvider : defaultPayloadProviders) {
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
