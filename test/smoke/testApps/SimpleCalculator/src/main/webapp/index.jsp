<%@page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>
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
	<input name="leftOperand" />
	<select name="operator">
		<option value="plus">+</option>
		<option value="minus">−</option>
		<!-- TODO: add other binary operators -->
	</select>
	<input name="rightOperand" />
	<input type="submit" />
</form>
<%--
<h2>History</h2>
<table>
<tbody>
<% 
	// FIXME: this doesn't work for some reason
	CalculationHistoryService service = new CalculationHistoryService();
	Collection<TimestampedBinaryCalculation> history = service.getHistoryEntries();
	DateFormat dfmt = new SimpleDateFormat();
	for (TimestampedBinaryCalculation entry : history) {
		BinaryCalculation bc = entry.getCalculation();
%>
<tr>
<td><%=dfmt.format(entry.getTimestamp())%></td>
<td align="right"><%=String.format("<i>%s</i> %s <i>%s</i> =", bc.getLeftOperandFormatted(), bc.getOperatorSymbol(), bc.getRightOperandFormatted())%></td>
<td align="left"><b><%=bc.resultFormatted() %></b></td>
</tr>
<%
	}
%>
</tbody>
</table>
--%>
</body>
</html>