<%@page import="com.microsoft.applicationinsights.TelemetryClient"%>

<%@page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>

<!DOCTYPE html>
<html>
<head>
<title>Simple Telemetry</title>
</head>
<body>
	<h1>trackPageView Test</h1>
	<%
		TelemetryClient tc = new TelemetryClient();
		tc.trackPageView("doPageView");
	%>
</body>
</html>