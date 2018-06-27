<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html title="Welcome">
<head>
    <!--
    To collect end-user usage analytics about your application,
    insert the following script into each page you want to track.
    Place this code immediately before the closing </head> tag,
    and before any other scripts. Your first data will appear
    automatically in just a few seconds.
    -->
    <script type="text/javascript">
      var appInsights = window.appInsights || function (config) {
        function s(config) {
          t[config] = function () {
            var i = arguments;
            t.queue.push(function () {
              t[config].apply(t, i)
            })
          }
        }

        var t = {config: config}, r = document, f = window, e = "script", o = r.createElement(e), i,
            u;
        for (o.src = config.url
            || "//az416426.vo.msecnd.net/scripts/a/ai.0.js", r.getElementsByTagName(
            e)[0].parentNode.appendChild(o), t.cookie = r.cookie, t.queue = [], i = ["Event",
          "Exception", "Metric", "PageView", "Trace"]; i.length;) s("track" + i.pop());
        return config.disableExceptionTracking || (i = "onerror", s("_"
            + i), u = f[i], f[i] = function (config, r, f, e, o) {
          var s = u && u(config, r, f, e, o);
          return s !== !0 && t["_" + i](config, r, f, e, o), s
        }), t
      }({
        instrumentationKey: "ef5c9284-7c19-4c97-805f-3cf38e652a1a"
      });

      window.appInsights = appInsights;
      appInsights.trackPageView();
    </script>
</head>
<body bgcolor="#f5deb3">

<br>
<br>
<h1 align="center">Hello <u>${name}</u></h1>

<div style="font-size:22px" align="center">
    <br>
    <br>
    <br>
    <br>
    <!--	<a href="/JBS/categories">Enter The Library</a>-->
    <a href="categories">Enter The Library</a>
</div>

</body>
</html>