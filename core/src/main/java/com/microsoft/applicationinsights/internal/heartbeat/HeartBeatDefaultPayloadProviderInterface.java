package com.microsoft.applicationinsights.internal.heartbeat;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * <h1>Interface for setting default properties</h1>
 *
 * <p>
 *   This interface is used to set the default payload of a provider and defines implementation for
 *   some helper methods to assist it.
 *
 *   The default concrete implementations are {@link com.microsoft.applicationinsights.internal.heartbeat.BaseDefaultHeartbeatPropertyProvider}
 *   and {@link com.microsoft.applicationinsights.internal.heartbeat.WebAppsDefaultHeartbeatProvider}
 * </p>
 *
 * @author Dhaval Doshi
 * @since 03-30-2018
 */
public interface HeartBeatDefaultPayloadProviderInterface {

  /**
   * Returns the name of the heartbeat provider.
   * @return Name of the heartbeat provider
   */
  String getName();

  /**
   * Tells if the input string is a reserved property.
   * @param keyword string to test
   * @return true if input string is reserved keyword
   */
  boolean isKeyWord(String keyword);

  /**
   * Returns a callable which can be executed to set the payload based on the parameters.
   * @param disableFields List of Properties to be excluded from payload
   * @param provider The current heartbeat provider
   * @return Callable which can be executed to add the payload
   */
  Callable<Boolean> setDefaultPayload(List<String> disableFields, HeartBeatProviderInterface provider);

}
