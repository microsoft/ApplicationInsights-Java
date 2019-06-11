package com.microsoft.ajl.simplecalc;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletFuncs {

	protected static void geRrenderHtml(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html;charset=UTF-8");
		renderHtml(request, response.getWriter());
	}

	private static void renderHtml(HttpServletRequest req, PrintWriter writer) {
		writer.println("<html>");
		writer.println("<head><title>Calculation Result</title></head>");
		writer.println("<body>");
		writer.printf("<h1>%s</h1>", req.getRequestURI());
		writer.println("<h2>OK!</h2>");
		writer.println("</body></html>");
	}
}
