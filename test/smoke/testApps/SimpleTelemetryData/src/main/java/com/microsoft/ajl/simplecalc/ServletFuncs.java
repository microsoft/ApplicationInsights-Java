package com.microsoft.ajl.simplecalc;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.ajl.simplecalc.model.BinaryCalculation;
import com.microsoft.ajl.simplecalc.model.BinaryOperator;

import static com.microsoft.ajl.simplecalc.ParameterConstants.*;

public class ServletFuncs {

	protected static void geRrenderHtml(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		BinaryCalculation bc = null;
		try {
			bc = readParameters(request.getParameterMap());
		} catch (CalculatorParameterException cpe) {
			String errMsg = cpe.getLocalizedMessage();
			System.err.println(errMsg);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
		}

		if (bc == null) {
			System.out.println("No parameters given.");
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return;
		}

		response.setContentType("text/html;charset=UTF-8");

		renderHtml(bc, response.getWriter());
	}

	/**
	 * @param parameterMap
	 * @return null if parameterMap is empty
	 */
	private static BinaryCalculation readParameters(Map<String, String[]> parameterMap)
			throws CalculatorParameterException {
		if (parameterMap == null) {
			throw new IllegalArgumentException("parameterMap cannot be null");
		}

		if (parameterMap.isEmpty()) {
			return null;
		}

		// log params
		System.out.println("Given parameters:");
		for (Entry<String, String[]> entry : parameterMap.entrySet()) {
			String pname = entry.getKey();
			System.out.printf("%s: %s%n", pname, Arrays.toString(entry.getValue()));
		}

		String strLopnd = parameterMap.get(LEFT_OPERAND)[0];
		String strRopnd = parameterMap.get(RIGHT_OPERAND)[0];
		String strOprtr = parameterMap.get(OPERATOR)[0];

		double lopnd = parseParamOrThrow(strLopnd, "Left operand is not a number: %s");
		double ropnd = parseParamOrThrow(strRopnd, "Right operand is not a number: %s");

		BinaryOperator op = BinaryOperator.fromVerb(strOprtr);
		if (op == null) {
			throw new CalculatorParameterException("Unknown operator: " + strOprtr);
		}

		return new BinaryCalculation(lopnd, ropnd, op);
	}

	private static void renderHtml(BinaryCalculation calc, PrintWriter writer) {
		writer.println("<html>");
		writer.println("<head><title>Calculation Result</title></head>");
		writer.println("<body>");
		writer.printf("<i>%s</i> %s <i>%s</i> = <b>%s</b>%n", calc.getLeftOperandFormatted(), calc.getOperatorSymbol(),
				calc.getRightOperandFormatted(), calc.resultFormatted());
		writer.println("<p><a href=\"/SimpleCalculator/\">Do Another Calculation</a></p>");
		writer.println("</body></html>");
	}

	private static double parseParamOrThrow(String param, String errMsgFmt) throws CalculatorParameterException {
		try {
			return Double.parseDouble(param);
		} catch (NumberFormatException e) {
			throw new CalculatorParameterException(String.format("Left operand is not a number: %s", param), e);
		}
	}
}
