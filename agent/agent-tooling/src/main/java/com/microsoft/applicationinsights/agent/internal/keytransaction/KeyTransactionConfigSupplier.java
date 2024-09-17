package com.microsoft.applicationinsights.agent.internal.keytransaction;

import com.azure.json.JsonProviders;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

public class KeyTransactionConfigSupplier implements Supplier<List<KeyTransactionConfig>> {

  public static final boolean KEY_TRANSACTIONS_ENABLED = true;

  // TODO remove reliance on global
  private static final KeyTransactionConfigSupplier instance = new KeyTransactionConfigSupplier();

//  static {
//    instance.set(hardcodedDemo());
//  }

  public static KeyTransactionConfigSupplier getInstance() {
    return instance;
  }

  private volatile List<KeyTransactionConfig> configs;

  @Override
  public List<KeyTransactionConfig> get() {
    return configs;
  }

  public void set(List<KeyTransactionConfig> configs) {
    this.configs = configs;
  }

  @SuppressWarnings("unused")
  private static List<KeyTransactionConfig> hardcodedDemo() {
    List<KeyTransactionConfig> configs;
    try {
      configs = NewResponse.fromJson(JsonProviders.createReader(
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
              + "            \"Field\": \"messaging.todo\",\n"
              + "            \"Operator\": \"==\",\n"
              + "            \"Value\": \"todo\"\n"
              + "          }\n"
              + "        ]\n"
              + "      }\n"
              + "    }\n"
              + "  ]\n"
              + "}\n"
          ))
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
