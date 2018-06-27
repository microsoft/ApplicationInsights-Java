<%@page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" %>
<%--<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.DateFormat"%>
<%@page import="com.microsoft.ajl.simplecalc.model.BinaryCalculation"%>
<%@page import="com.microsoft.ajl.simplecalc.model.BinaryCalculation.TimestampedBinaryCalculation"%>
<%@page import="java.util.Collection"%>
<%@page import="com.microsoft.ajl.simplecalc.CalculationHistoryService;"%>--%>
<!DOCTYPE html>
<html>
<head>
    <title>Simple Calculator</title>
</head>
<body>
<h1>Simple Calculator</h1>
<form action="doCalc" autocomplete="off">
    <input name="leftOperand"/>
    <select name="operator">
        <option value="plus">+</option>
        <option value="minus">−</option>
        <!-- TODO: add other binary operators -->
    </select>
    <input name="rightOperand"/>
    <input type="submit"/>
</form>
</body>
</html>