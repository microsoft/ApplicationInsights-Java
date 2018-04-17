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
  private static final List<HeartBeatPayloadProviderInterface> defaultPayloadProviders =
      new ArrayList<>();

  static {
    defaultPayloadProviders.add(new DefaultHeartBeatPropertyProvider());
    defaultPayloadProviders.add(new WebAppsHeartbeatProvider());
  }

  /**
   * Returns true if the input string is reserved keyword in any of the providers
   * @param keyword string to test
   * @return true if keyword in providers
   */
  public static boolean isDefaultKeyword(String keyword) {
    for (HeartBeatPayloadProviderInterface payloadProvider : defaultPayloadProviders) {
      if (payloadProvider.isKeyword(keyword)) {
        return true;
      }
    }
    return false;
  }

  /**
   * This is used to add additional default providers. Used in SpringBoot Module
   * @param payloadProviderInterface
   * @return true if payloadProvider is added successfully
   */
  public static boolean addDefaultPayLoadProvider(HeartBeatPayloadProviderInterface payloadProviderInterface) {
    if (payloadProviderInterface != null) {
      defaultPayloadProviders.add(payloadProviderInterface);
      return true;
    }
    return false;
  }

  /**
   * Callable which delegates calls to providers for adding payload.
   * @param disabledFields the properties which are disabled by user
   * @param disabledProviders providers which are disabled by users
   * @param provider The HeartBeat provider
   * @return Callable to perform execution
   */
  public static Callable<Boolean> populateDefaultPayload(final List<String> disabledFields, final List<String>
      disabledProviders, final HeartBeatProviderInterface provider) {
    return new Callable<Boolean>() {

      volatile boolean populatedFields = false;
      @Override
      public Boolean call() throws Exception {
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
