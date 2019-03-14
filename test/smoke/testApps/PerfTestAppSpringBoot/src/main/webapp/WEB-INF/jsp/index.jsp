<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>PerfTestApp</title>
</head>
<body>
<h1>PerfTestApp</h1>
<p>
    Each endpoint will respond with the number of milliseconds the request took.
</p>
<h2><a href="./baseline">Baseline</a></h2>
<p>
    This endpoint simulates an application without the Java SDK. It performs some simple computations to simulate something close to a real application.
    <strong>When doing a comparison with track/*, this should be run first.</strong>
</p>

<h2>Track/*</h2>
<p>
    Each method performs the same "churn" computations done in the baseline (there are iterations performed.
</p>
<h3>"Helpers"</h3>
<p>These are the methods which take values and not <em>Abc</em>Telemetry objects.</p>
<ul>
    <!-- TODO provide values for parameters sent to methods -->
    <li><a href="./track/metric/helper/aggregate">Metric, Aggregate</a></li>
    <li><a href="./track/metric/helper/measurement">Metric, Measurement</a></li>
    <li><a href="./track/event">Event</a></li>
    <li><a href="./track/httpRequest">HttpRequest</a></li>
    <li><a href="./track/dependency">Dependency</a></li>
    <li><a href="./track/trace">Trace</a></li>
    <li><a href="./track/exception">Exception</a></li>
    <li><a href="./track/pageView">PageView</a></li>
</ul>
<h3>"Full"</h3>
<p>These are the methods which take Telemetry objects.</p>
<ul>
    <li><a href="./track/metric/full/aggregate">Metric, Aggregate</a></li>
    <li><a href="./track/metric/full/measurement">Metric, Measurement</a></li>
    <li><a href="./track/request/full">Request</a></li>
    <li><a href="./track/pageView/full">PageView</a></li>
    <li><a href="./track/dependency/full">Dependency</a></li>
</ul>
<h2><a href="./fakeRest">FakeRest Endpoint</a></h2>
<p>
    This simulates a "real world" endpoint. It runs trackDependnecy, trackEvent, trackMetric and trackTrace.
</p>
</body>
</html>
