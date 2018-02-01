<%@page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>

<!DOCTYPE html>
<html>
<head>
<title>Simple Telemetry</title>
</head>
<body>
<h1>Simple Telemetry</h1>
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

<form action="trackTrace" autocomplete="off">
	<input name="leftOperand" />
	<select name="operator">
		<option value="plus">+</option>
		<option value="minus">−</option>
		<!-- TODO: add other binary operators -->
	</select>
	<input name="rightOperand" />
	<input type="submit" />
</form>

<form action="trackException" autocomplete="off">
	<input name="leftOperand" />
	<select name="operator">
		<option value="plus">+</option>
		<option value="minus">−</option>
		<!-- TODO: add other binary operators -->
	</select>
	<input name="rightOperand" />
	<input type="submit" />
</form>

</body>
</html>