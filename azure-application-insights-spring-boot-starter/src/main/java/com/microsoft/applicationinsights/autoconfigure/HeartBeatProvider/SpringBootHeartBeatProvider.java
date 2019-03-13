package com.microsoft.applicationinsights.autoconfigure.HeartBeatProvider;

import com.microsoft.applicationinsights.internal.heartbeat.HeartBeatPayloadProviderInterface;
import com.microsoft.applicationinsights.internal.heartbeat.HeartBeatProviderInterface;
import com.microsoft.applicationinsights.internal.heartbeat.MiscUtils;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;

/**
 * <h1>SpringBoot Heartbeat Property Provider</h1>
 * <p>
 *   This class is a concrete implementation of {@link HeartBeatPayloadProviderInterface}
 *   It enables setting SpringBoot Metadata to heartbeat payload.
 * </p>
 *
 * @author Dhaval Doshi
 */
public class SpringBootHeartBeatProvider implements HeartBeatPayloadProviderInterface {

  /**
   * Collection holding default properties for this default provider.
   */
  private final Set<String> defaultFields;

  /**
   * Name of this provider.
   */
  private final String name = "SpringBootProvider";

  private final String SPRING_BOOT_VERSION = "ai.spring-boot.version";

  private final String SPRING_VERSION = "ai.spring.version";

  private final String SPRING_BOOT_STARTER_VERSION = "ai.spring.boot.starter.version";



  public SpringBootHeartBeatProvider() {
    defaultFields = new HashSet<>();
    initializeDefaultFields(defaultFields);
  }

  @Override

  public String getName() {
    return this.name;
  }

  @Override
  public boolean isKeyword(String keyword) {
    return defaultFields.contains(keyword);
  }

  @Override
  public Callable<Boolean> setDefaultPayload(final List<String> disableFields,
      final HeartBeatProviderInterface provider) {
    return new Callable<Boolean>() {

      Set<String> enabledProperties = MiscUtils.except(defaultFields, disableFields);
      @Override
      public Boolean call() {
        boolean hasSetValues = false;
        for (String fieldName : enabledProperties) {
          try {
            switch (fieldName) {
              case SPRING_BOOT_VERSION:
                provider.addHeartBeatProperty(fieldName, getSpringBootVersion(), true);
                hasSetValues = true;
                break;
              case SPRING_VERSION:
                provider.addHeartBeatProperty(fieldName, getSpringVersion(), true);
                hasSetValues = true;
                break;
              case SPRING_BOOT_STARTER_VERSION:
                provider.addHeartBeatProperty(fieldName, getSpringBootStarterVersionNumber(), true);
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
    defaultFields.add(SPRING_BOOT_VERSION);
    defaultFields.add(SPRING_VERSION);
    defaultFields.add(SPRING_BOOT_STARTER_VERSION);
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
   * @return the SpringFrameWork version String
   */
  private String getSpringVersion() {
    return SpringVersion.getVersion();
  }

  /**
   * Gets the AI SpringBoot starter version number
   * @return the AI SpringBoot starter version number
   */
  private String getSpringBootStarterVersionNumber() {
    Properties starterVersionProperties = PropertyHelper.getStarterVersionProperties();
    if (starterVersionProperties != null) {
      return starterVersionProperties.getProperty("version");
    }
    return "undefined";
  }

}
