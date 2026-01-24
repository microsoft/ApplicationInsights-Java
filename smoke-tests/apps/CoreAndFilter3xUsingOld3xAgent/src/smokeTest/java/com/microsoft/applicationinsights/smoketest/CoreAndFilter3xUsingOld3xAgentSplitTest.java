package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;

public class CoreAndFilter3xUsingOld3xAgentSplitTest {

	@Environment(TOMCAT_8_JAVA_17)
	static class Tomcat8Java17Test extends CoreAndFilter3xUsingOld3xAgentTest {}

	@Environment(TOMCAT_8_JAVA_17_OPENJ9)
	static class Tomcat8Java17OpenJ9Test extends CoreAndFilter3xUsingOld3xAgentTest {}

	// note: old 3.x agents don't support Java 19

	@Environment(WILDFLY_13_JAVA_8)
	static class Wildfly13Java8Test extends CoreAndFilter3xUsingOld3xAgentTest {}

	@Environment(WILDFLY_13_JAVA_8_OPENJ9)
	static class Wildfly13Java8OpenJ9Test extends CoreAndFilter3xUsingOld3xAgentTest {}
}
