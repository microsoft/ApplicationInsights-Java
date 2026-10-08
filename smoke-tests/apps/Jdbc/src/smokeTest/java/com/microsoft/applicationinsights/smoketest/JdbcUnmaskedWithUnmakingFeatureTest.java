// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;

@UseAgent("unmasked_applicationinsights.json")
@Environment(TOMCAT_8_JAVA_8)
class JdbcUnmaskedWithUnmakingFeatureTest extends AbstractJdbcUnmasked {}
