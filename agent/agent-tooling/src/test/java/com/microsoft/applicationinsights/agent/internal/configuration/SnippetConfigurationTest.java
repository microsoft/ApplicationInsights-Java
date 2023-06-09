// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class SnippetConfigurationTest {
  private static final String CONNECTION_STRING =
      "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fake-ingestion-endpoint";

  @Test
  void testSnippetSetup() throws IOException {
    String snippet = SnippetConfiguration.readSnippet();
    assertThat(snippet).contains("connectionString: \"YOUR_CONNECTION_STRING\"\n");
    snippet = snippet.replace("YOUR_CONNECTION_STRING", CONNECTION_STRING);
    assertThat(snippet).contains("    connectionString: \"" + CONNECTION_STRING);
  }
}
