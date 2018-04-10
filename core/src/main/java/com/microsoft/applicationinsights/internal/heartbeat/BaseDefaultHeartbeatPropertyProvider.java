package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <h1>Base Heartbeat Property Provider</h1>
 *
 * <p>
 *   This class is a concrete implementation of {@link com.microsoft.applicationinsights.internal.heartbeat.HeartBeatDefaultPayloadProviderInterface}
 *   It enables setting SDK Metadata to heartbeat payload.
 * </p>
 *
 * @author Dhaval Doshi
 * @since 03-30-2018
 */
public class BaseDefaultHeartbeatPropertyProvider implements HeartBeatDefaultPayloadProviderInterface {

  /**
   * Collection holding default properties for this default provider.
   */
  private final Set<String> defaultFields;

  /**
   * Random GUID that would help in analysis when app has stopped and restarted. Each restart will
   * have a new GUID. If the application is unstable and goes through frequent restarts this will help
   * us identify instability in the analytics backend.
   */
  private static UUID uniqueProcessId;

  /**
   * Name of this provider.
   */
  private final String name = "Base";

  public BaseDefaultHeartbeatPropertyProvider() {
    defaultFields = new HashSet<>();
    initializeDefaultFields(defaultFields);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public boolean isKeyWord(String keyword) {
    return defaultFields.contains(keyword);
  }

  @Override
  public Callable<Boolean> setDefaultPayload(final List<String> disableFields,
      final HeartBeatProviderInterface provider) {
    return new Callable<Boolean>() {

      // using volatile here to avoid caching in threads.
      volatile boolean hasSetValues = false;
      volatile Set<String> enabledProperties = MiscUtils.except(defaultFields, disableFields);
      @Override
      public Boolean call() {
        for (String fieldName : enabledProperties) {
          try {
            switch (fieldName) {
              case "jdkVersion":
                provider.addHeartBeatProperty(fieldName, getJdkVersion(), true);
                hasSetValues = true;
                break;
              case "sdk-version":
                provider.addHeartBeatProperty(fieldName, getSdkVersion(), true);
                hasSetValues = true;
                break;
              case "osType":
                provider.addHeartBeatProperty(fieldName, getOsType(), true);
                hasSetValues = true;
                break;
              case "processSessionId":
                provider.addHeartBeatProperty(fieldName, getProcessSessionId(), true);
                hasSetValues = true;
                break;
              default:
                //We won't accept unknown properties in default providers.
                InternalLogger.INSTANCE.trace("Encountered unknown default property");
                break;
            }
          }
          catch (Exception e) {
           InternalLogger.INSTANCE.warn("Failed to obtain heartbeat property, stack trace"
               + "is: %s", ExceptionUtils.getStackTrace(e));
          }
        }
        return hasSetValues;
      }
    };
  }

  /**
   * This method initializes the collection with Default Properties of this provider.
   * @param defaultFields collection to hold default properties.
   */
  private void initializeDefaultFields(Set<String> defaultFields) {

    if (defaultFields == null) {
      defaultFields = new HashSet<>();
    }
    defaultFields.add("jdkVersion");
    defaultFields.add("sdk-version");
    defaultFields.add("osType");
    defaultFields.add("processSessionId");
  }

  /**
   * Gets the JDK version being used by the application
   * @return String representing JDK Version
   */
  private String getJdkVersion() {
    return System.getProperty("java.version");
  }

  /**
   * Returns the Application Insights SDK version user is using to instrument his application
   * @return returns string prefixed with "java" representing the Application Insights Java
   * SDK version.
   */
  private String getSdkVersion() {

    String sdkVersion = "java";

    Properties sdkVersionProps = PropertyHelper.getSdkVersionProperties();
    if (sdkVersionProps != null) {
      String version = sdkVersionProps.getProperty("version");
      sdkVersion = String.format("java:%s", version);
      return sdkVersion;
    }
    return sdkVersion + ":unknown-version";
  }

  /**
   * Gets the OS version on which application is running.
   * @return String representing OS version
   */
  private String getOsType() {
    return System.getProperty("os.name");
  }

  /**
   * Returns the Unique GUID for the application's current session.
   * @return String representing GUID for each running session
   */
  private String getProcessSessionId() {
    if (uniqueProcessId == null) {
      uniqueProcessId = UUID.randomUUID();
    }
    return uniqueProcessId.toString();
  }
}
