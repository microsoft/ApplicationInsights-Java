/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.azure.monitor.opentelemetry.exporter.implementation.heartbeat;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 * <h1>WebApp Heartbeat Property Provider</h1>
 *
 * <p>This class is a concrete implementation of {@link HeartBeatPayloadProviderInterface} It
 * enables setting Web-apps Metadata to heartbeat payload.
 */
public class WebAppsHeartbeatProvider implements HeartBeatPayloadProviderInterface {

  private static final ClientLogger logger = new ClientLogger(WebAppsHeartbeatProvider.class);

  /** Collection holding default properties for this default provider. */
  private final Set<String> defaultFields;

  /** Map for storing environment variables. */
  private Map<String, String> environmentMap;

  private static final String WEBSITE_SITE_NAME = "appSrv_SiteName";

  private static final String WEBSITE_HOSTNAME = "appSrv_wsHost";

  private static final String WEBSITE_HOME_STAMPNAME = "appSrv_wsStamp";

  private static final String WEBSITE_OWNER_NAME = "appSrv_wsOwner";

  private static final String WEBSITE_RESOURCE_GROUP = "appSrv_ResourceGroup";
  // Only populated in Azure functions
  private static final String WEBSITE_SLOT_NAME = "appSrv_SlotName";

  /** Constructor that initializes fields and load environment variables. */
  public WebAppsHeartbeatProvider() {
    defaultFields = new HashSet<>();
    environmentMap = System.getenv();
    initializeDefaultFields(defaultFields);
  }

  @Override
  public Runnable setDefaultPayload(HeartbeatExporter provider) {
    return new Runnable() {

      final Set<String> enabledProperties = defaultFields;

      @Override
      public void run() {

        // update environment variable to account for
        updateEnvironmentVariableMap();
        for (String fieldName : enabledProperties) {
          try {
            switch (fieldName) {
              case WEBSITE_SITE_NAME:
                String webSiteName = getWebsiteSiteName();
                if (Strings.isNullOrEmpty(webSiteName)) {
                  break;
                }
                provider.addHeartBeatProperty(fieldName, webSiteName, true);
                break;
              case WEBSITE_HOSTNAME:
                String webSiteHostName = getWebsiteHostName();
                if (Strings.isNullOrEmpty(webSiteHostName)) {
                  break;
                }
                provider.addHeartBeatProperty(fieldName, webSiteHostName, true);
                break;
              case WEBSITE_HOME_STAMPNAME:
                String websiteHomeStampName = getWebsiteHomeStampName();
                if (Strings.isNullOrEmpty(websiteHomeStampName)) {
                  break;
                }
                provider.addHeartBeatProperty(fieldName, websiteHomeStampName, true);
                break;
              case WEBSITE_OWNER_NAME:
                String websiteOwnerName = getWebsiteOwnerName();
                if (Strings.isNullOrEmpty(websiteOwnerName)) {
                  break;
                }
                provider.addHeartBeatProperty(fieldName, websiteOwnerName, true);
                break;
              case WEBSITE_RESOURCE_GROUP:
                String websiteResourceGroup = getWebsiteResourceGroup();
                if (Strings.isNullOrEmpty(websiteResourceGroup)) {
                  break;
                }
                provider.addHeartBeatProperty(fieldName, websiteResourceGroup, true);
                break;
              case WEBSITE_SLOT_NAME:
                String websiteSlotName = getWebsiteSlotName();
                if (Strings.isNullOrEmpty(websiteSlotName)) {
                  break;
                }
                provider.addHeartBeatProperty(fieldName, websiteSlotName, true);
                break;
              default:
                logger.verbose("Unknown web apps property encountered");
                break;
            }
          } catch (RuntimeException e) {
            logger.warning("Failed to obtain heartbeat property", e);
          }
        }
      }
    };
  }

  /** Populates the default Fields with the properties. */
  private static void initializeDefaultFields(Set<String> defaultFields) {
    defaultFields.add(WEBSITE_SITE_NAME);
    defaultFields.add(WEBSITE_HOSTNAME);
    defaultFields.add(WEBSITE_HOME_STAMPNAME);
    defaultFields.add(WEBSITE_OWNER_NAME);
    defaultFields.add(WEBSITE_RESOURCE_GROUP);
    defaultFields.add(WEBSITE_SLOT_NAME);
  }

  /** Returns the name of the website by reading environment variable. */
  private String getWebsiteSiteName() {
    return environmentMap.get("WEBSITE_SITE_NAME");
  }

  /** Returns the website host name by reading environment variable. */
  private String getWebsiteHostName() {
    return environmentMap.get("WEBSITE_HOSTNAME");
  }

  /** Returns the website home stamp name by reading environment variable. */
  private String getWebsiteHomeStampName() {
    return environmentMap.get("WEBSITE_HOME_STAMPNAME");
  }

  /** Returns the website owner name by reading environment variable. */
  private String getWebsiteOwnerName() {
    return environmentMap.get("WEBSITE_OWNER_NAME");
  }

  /** Returns the website resource group by reading environment variable. */
  private String getWebsiteResourceGroup() {
    return environmentMap.get("WEBSITE_RESOURCE_GROUP");
  }

  /** Returns the website slot name by reading environment variable. */
  private String getWebsiteSlotName() {
    return environmentMap.get("WEBSITE_SLOT_NAME");
  }

  /**
   * This method updates the environment variable at every call to add the payload, to cover hotswap
   * scenarios.
   */
  private void updateEnvironmentVariableMap() {
    environmentMap = System.getenv();
  }
}
