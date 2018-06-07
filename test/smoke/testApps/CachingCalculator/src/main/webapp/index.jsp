<%@page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>
<%@page import="redis.clients.jedis.Jedis"%>
<!DOCTYPE html>
<html>
<head>
<title>Simple Calculator</title>
</head>
<body>
<h1>Simple Calculator</h1>
<form action="doCalc" autocomplete="off">
	<%
		String loperand = "";
		String oprtr = "plus";
		String roperand = "";
		String redisHostname = System.getenv("REDIS");
		Jedis redis = null;
		if (redisHostname != null) {
			try {
				redis = new Jedis(redisHostname, 6379);
				if (redis.exists("loperand") && redis.exists("operator") && redis.exists("roperand")) {
					loperand = redis.get("loperand");
					oprtr = redis.get("operator");
					roperand = redis.get("roperand");
				}
			} catch (Exception e) {
				System.err.println("Something went wrong with redis");
				e.printStackTrace();
			}
		}
	%>
	<input name="leftOperand" value="<%=loperand %>" required />
	<select name="operator">
		<option value="plus" <%="plus".equals(oprtr) ? "selected" : "" %> >+</option>
		<option value="minus" <%="minus".equals(oprtr) ? "selected" : "" %> >−</option>
		<!-- TODO: add other binary operators -->
	</select>
	<input name="rightOperand" value="<%=roperand %>" required />
	<input type="submit" />
</form>
<p>Redis status (<%=redisHostname %>): <%=redis == null ? "DOWN" : "UP" %></p>
</body>
</html>