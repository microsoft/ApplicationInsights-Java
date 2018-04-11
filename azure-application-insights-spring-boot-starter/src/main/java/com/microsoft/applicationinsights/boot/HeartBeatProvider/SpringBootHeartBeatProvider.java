package com.microsoft.applicationinsights.boot.HeartBeatProvider;

import com.microsoft.applicationinsights.internal.heartbeat.HeartBeatDefaultPayloadProviderInterface;
import com.microsoft.applicationinsights.internal.heartbeat.HeartBeatProviderInterface;
import com.microsoft.applicationinsights.internal.heartbeat.MiscUtils;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;
import org.springframework.core.env.Environment;

public class SpringBootHeartBeatProvider implements HeartBeatDefaultPayloadProviderInterface {

  /**
   * Collection holding default properties for this default provider.
   */
  private final Set<String> defaultFields;

  /**
   * Name of this provider.
   */
  private final String name = "SpringBootProvider";

  private final Environment environment;

  public SpringBootHeartBeatProvider(Environment environment) {
    defaultFields = new HashSet<>();
    this.environment = environment;
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
              case "ai.spring-boot.version":
                provider.addHeartBeatProperty(fieldName, getSpringBootVersion(), true);
                hasSetValues = true;
                break;
              case "ai.spring.version":
                provider.addHeartBeatProperty(fieldName, getSpringVersion(), true);
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
    defaultFields.add("ai.spring-boot.version");
    defaultFields.add("ai.spring.version");
  }

  /**
   * Gets the version of SpringBoot
   * @return returns springboot version string
   */
  private String getSpringBootVersion() {
    return SpringBootVersion.getVersion();
  }

  /**
   * Gets the Spring Framework version
   * @return returns the SpringFrameWork version String
   */
  private String getSpringVersion() {
    return SpringVersion.getVersion();
  }

}
