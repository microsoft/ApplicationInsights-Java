<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html title="Books">
<head>
<!--
To collect end-user usage analytics about your application,
insert the following script into each page you want to track.
Place this code immediately before the closing </head> tag,
and before any other scripts. Your first data will appear
automatically in just a few seconds.
-->
<script type="text/javascript">
    var appInsights=window.appInsights||function(config){
        function s(config){t[config]=function(){var i=arguments;t.queue.push(function(){t[config].apply(t,i)})}}var t={config:config},r=document,f=window,e="script",o=r.createElement(e),i,u;for(o.src=config.url||"//az416426.vo.msecnd.net/scripts/a/ai.0.js",r.getElementsByTagName(e)[0].parentNode.appendChild(o),t.cookie=r.cookie,t.queue=[],i=["Event","Exception","Metric","PageView","Trace"];i.length;)s("track"+i.pop());return config.disableExceptionTracking||(i="onerror",s("_"+i),u=f[i],f[i]=function(config,r,f,e,o){var s=u&&u(config,r,f,e,o);return s!==!0&&t["_"+i](config,r,f,e,o),s}),t
    }({
        instrumentationKey:"ef5c9284-7c19-4c97-805f-3cf38e652a1a"
    });
    
    window.appInsights=appInsights;
    appInsights.trackPageView();
</script>
</head>
<body background="./resources/Books.jpg">
<br>
<br>
<h2 align="center">${subject} Books</h2>

<div>
    <table align="center" style="background-color: wheat; border:1px solid black; ; font-size: 24px" cellspacing="7" cellpadding="7">
        <thead>
        <tr bgcolor="#deb887">
            <td>Title</td>
            <td>Author</td>
            <td>ISBN-10</td>
            <td>Already Borrowed</td>
            <td>Action</td>
        </tr>
        </thead>
        <c:forEach var="bookData" items="${books}" varStatus="loopStatus">
        <tr bgcolor="${loopStatus.index % 2 == 0 ? '#aaaaaa' : '#cccccc'}">
            <td align="center"><c:out value="${bookData.book.title}"/></td>
            <td align="center"><c:out value="${bookData.book.author}"/></td>
            <td align="center"><c:out value="${bookData.book.isbn10}"/></td>

            <c:choose>
                <c:when test="${bookData.loaned == 'true'}">
                    <td align="center" bgcolor="#7fff00"><c:out value="${bookData.loaned}"/></td>
                </c:when>
                <c:otherwise>
                    <td align="center"><c:out value="${bookData.loaned}"/></td>
                </c:otherwise>
            </c:choose>

            <td align="center"><a href='loan?title=${bookData.book.title}&id=<c:out value="${bookData.book.isbn10}"/>&subject=${subject}'>Borrow this book!</a></td>
        </tr>
        </c:forEach>

    </table>
    <br>
    <br>
    <br>
    <br>
    <br>
    <br>
    <br>
    <br>
    <br>
        <div align="center" style="color: red; font-size: 24px">
            <a href="categories">Back to 'Categories'</a>
        </div>

</div>

</html>