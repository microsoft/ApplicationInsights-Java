// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.keytransaction;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.azure.json.JsonProviders;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class KeyTransactionConfigSupplier implements Supplier<List<KeyTransactionConfig>> {

  public static final boolean KEY_TRANSACTIONS_ENABLED = true;
  public static final boolean USE_HARDCODED_CONFIG = false;

  // TODO remove reliance on global
  private static final KeyTransactionConfigSupplier instance = new KeyTransactionConfigSupplier();

  static {
    if (USE_HARDCODED_CONFIG) {
      instance.set(hardcodedDemo());
    }
  }

  public static KeyTransactionConfigSupplier getInstance() {
    return instance;
  }

  private volatile List<KeyTransactionConfig> configs = emptyList();

  private KeyTransactionConfigSupplier() {}

  @Override
  public List<KeyTransactionConfig> get() {
    return configs;
  }

  public void set(List<KeyTransactionConfig> configs) {
    this.configs = configs;
  }

  private static List<KeyTransactionConfig> hardcodedDemo() {
    List<KeyTransactionConfig> configs;
    try {
      configs =
          NewResponse.fromJson(
                  JsonProviders.createReader(
                      "{\n"
                          + "  \"itemsReceived\": 13,\n"
                          + "  \"itemsAccepted\": 13,\n"
                          + "  \"appId\": null,\n"
                          + "  \"errors\": [],\n"
                          + "  \"sdkConfiguration\": [\n"
                          + "    {\n"
                          + "      \"Key\": \"Transaction\",\n"
                          + "      \"Value\": {\n"
                          + "        \"Name\": \"EarthOrbit\",\n"
                          + "        \"StartCriteria\": [\n"
                          + "          {\n"
                          + "            \"Field\": \"url.path\",\n"
                          + "            \"Operator\": \"==\",\n"
                          + "            \"Value\": \"/earth\"\n"
                          + "          }\n"
                          + "        ],\n"
                          + "        \"EndCriteria\": []\n"
                          + "      }\n"
                          + "    },\n"
                          + "    {\n"
                          + "      \"Key\": \"Transaction\",\n"
                          + "      \"Value\": {\n"
                          + "        \"Name\": \"MarsMission\",\n"
                          + "        \"StartCriteria\": [\n"
                          + "          {\n"
                          + "            \"Field\": \"url.path\",\n"
                          + "            \"Operator\": \"==\",\n"
                          + "            \"Value\": \"/mars\"\n"
                          + "          }\n"
                          + "        ],\n"
                          + "        \"EndCriteria\": [\n"
                          + "          {\n"
                          + "            \"Field\": \"messaging.operation\",\n"
                          + "            \"Operator\": \"==\",\n"
                          + "            \"Value\": \"process\"\n"
                          + "          },\n"
                          + "          {\n"
                          + "            \"Field\": \"messaging.destination.name\",\n"
                          + "            \"Operator\": \"==\",\n"
                          + "            \"Value\": \"space\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    }\n"
                          + "  ]\n"
                          + "}\n"))
              .getSdkConfigurations()
              .stream()
              .map(SdkConfiguration::getValue)
              .collect(toList());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return configs;
  }
}
