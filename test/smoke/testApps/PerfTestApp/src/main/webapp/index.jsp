<%@ page contentType="text/html;charset=UTF-8" language="java" %>
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
    This endpoint simulates an application without the Java SDK. It performs some simple
    computations to simulate something close to a real application.
    <strong>When doing a comparison with track/*, this should be run first.</strong>
</p>

<h2>Track/*</h2>
<p>
    Each method performs the same "churn" computations done in the baseline (there are iterations
    performed.
</p>
<ul>
    <!-- TODO provide values for parameters sent to methods -->
    <li><a href="./track/metric">Metric</a></li>
    <li><a href="./track/event">Event</a></li>
    <li><a href="./track/httpRequest">HttpRequest</a></li>
    <li><a href="./track/dependency">Dependency</a></li>
    <li><a href="./track/trace">Trace</a></li>
    <li><a href="./track/exception">Exception</a></li>
    <li><a href="./track/pageView">PageView</a></li>
</ul>
<h2><a href="./fakeRest">FakeRest Endpoint</a></h2>
<p>
    This simulates a "real world" endpoint. It runs trackDependnecy, trackEvent, trackMetric and
    trackTrace.
</p>
</body>
</html>
