package com.microsoft.applicationinsights.internal.heartbeat;

import java.util.List;
import java.util.concurrent.Callable;

/**
 *
 *
 * <h1>Interface for setting default properties</h1>
 *
 * <p>This interface is used to set the default payload of a provider and defines implementation for
 * some helper methods to assist it.
 *
 * <p>The default concrete implementations are {@link DefaultHeartBeatPropertyProvider} and {@link
 * WebAppsHeartbeatProvider}
 *
 * @author Dhaval Doshi
 */
public interface HeartBeatPayloadProviderInterface {

  /**
   * Returns the name of the heartbeat provider.
   *
   * @return Name of the heartbeat provider
   */
  String getName();

  /**
   * Tells if the input string is a reserved property.
   *
   * @param keyword string to test
   * @return true if input string is reserved keyword
   */
  boolean isKeyword(String keyword);

  /**
   * Returns a callable which can be executed to set the payload based on the parameters.
   *
   * @param disableFields List of Properties to be excluded from payload
   * @param provider The current heartbeat provider
   * @return Callable which can be executed to add the payload
   */
  Callable<Boolean> setDefaultPayload(
      List<String> disableFields, HeartBeatProviderInterface provider);
}
