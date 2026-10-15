// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.keytransaction;

import static com.microsoft.applicationinsights.agent.internal.keytransaction.KeyTransactionConfig.Operator.EQUALS;
import static org.assertj.core.api.Assertions.assertThat;

import com.azure.json.JsonProviders;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class KeyTransactionConfigTest {

  @Test
  void test() throws Exception {
    InputStream in = KeyTransactionConfigTest.class.getResourceAsStream("demo.json");
    NewResponse parsedResponse = NewResponse.fromJson(JsonProviders.createReader(in));

    assertThat(parsedResponse.getSdkConfigurations()).hasSize(2);

    SdkConfiguration earthOrbitSdkConfiguration = parsedResponse.getSdkConfigurations().get(0);
    assertThat(earthOrbitSdkConfiguration.getKey()).isEqualTo("Transaction");

    KeyTransactionConfig earthOrbitKeyTransactionConfig = earthOrbitSdkConfiguration.getValue();
    assertThat(earthOrbitKeyTransactionConfig.getName()).isEqualTo("EarthOrbit");

    assertThat(earthOrbitKeyTransactionConfig.getStartCriteria()).hasSize(1);
    KeyTransactionConfig.Criterion earthOrbitStartCriteria =
        earthOrbitKeyTransactionConfig.getStartCriteria().get(0);
    assertThat(earthOrbitStartCriteria.getField().getKey()).isEqualTo("url.path");
    assertThat(earthOrbitStartCriteria.getOperator()).isEqualTo(EQUALS);
    assertThat(earthOrbitStartCriteria.getValue()).isEqualTo("/earth");

    assertThat(earthOrbitKeyTransactionConfig.getEndCriteria()).isEmpty();

    SdkConfiguration marsMissionSdkConfiguration = parsedResponse.getSdkConfigurations().get(1);
    assertThat(marsMissionSdkConfiguration.getKey()).isEqualTo("Transaction");

    KeyTransactionConfig marsMissionKeyTransactionConfig = marsMissionSdkConfiguration.getValue();
    assertThat(marsMissionKeyTransactionConfig.getName()).isEqualTo("MarsMission");

    assertThat(marsMissionKeyTransactionConfig.getStartCriteria()).hasSize(1);
    KeyTransactionConfig.Criterion marsMissionStartCriteria =
        marsMissionKeyTransactionConfig.getStartCriteria().get(0);
    assertThat(marsMissionStartCriteria.getField().getKey()).isEqualTo("url.path");
    assertThat(marsMissionStartCriteria.getOperator()).isEqualTo(EQUALS);
    assertThat(marsMissionStartCriteria.getValue()).isEqualTo("/mars");

    assertThat(marsMissionKeyTransactionConfig.getEndCriteria()).hasSize(2);

    KeyTransactionConfig.Criterion marsMissionEndCriteria1 =
        marsMissionKeyTransactionConfig.getEndCriteria().get(0);
    assertThat(marsMissionEndCriteria1.getField().getKey()).isEqualTo("messaging.operation");
    assertThat(marsMissionEndCriteria1.getOperator()).isEqualTo(EQUALS);
    assertThat(marsMissionEndCriteria1.getValue()).isEqualTo("process");

    KeyTransactionConfig.Criterion marsMissionEndCriteria2 =
        marsMissionKeyTransactionConfig.getEndCriteria().get(1);
    assertThat(marsMissionEndCriteria2.getField().getKey()).isEqualTo("messaging.destination.name");
    assertThat(marsMissionEndCriteria2.getOperator()).isEqualTo(EQUALS);
    assertThat(marsMissionEndCriteria2.getValue()).isEqualTo("space");
  }
}
