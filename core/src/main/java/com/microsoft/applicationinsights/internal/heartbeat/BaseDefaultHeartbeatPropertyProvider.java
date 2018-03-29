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

public class BaseDefaultHeartbeatPropertyProvider implements HeartBeatDefaultPayloadProviderInterface {

  private final Set<String> defaultFields;

  private UUID uniqueProcessId;

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
  public Callable<Boolean> setDefaultPayload(final List<String> disableFields,
      final HeartBeatProviderInterface provider) {
    return new Callable<Boolean>() {

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

  private void initializeDefaultFields(Set<String> defaultFields) {

    if (defaultFields == null) {
      defaultFields = new HashSet<>();
    }
    defaultFields.add("jdkVersion");
    defaultFields.add("sdk-version");
    defaultFields.add("osType");
    defaultFields.add("processSessionId");
  }

  private String getJdkVersion() {
    return System.getProperty("java.version");
  }

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


  private String getOsType() {
    return System.getProperty("os.name");
  }

  private String getProcessSessionId() {
    if (this.uniqueProcessId == null) {
      uniqueProcessId = UUID.randomUUID();
    }
    return uniqueProcessId.toString();
  }
}
