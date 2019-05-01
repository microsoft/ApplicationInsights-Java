package com.microsoft.ajl.simplecalc;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Servlet implementation class SimpleTestRequestSlowWithResponseTime
 */
@WebServlet(description = "calls request slow w/o Thread.sleep", urlPatterns = "/slowLoop")
public class SlowRequestCpuBoundServlet extends HttpServlet {

    private static final long serialVersionUID = 3007663491446163538L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final long startTime = System.currentTimeMillis();

        ServletFuncs.geRrenderHtml(request, response);
        int responseTime = 25;
        final String customRepsonseTime = request.getParameter("responseTime");
        if (StringUtils.isNotBlank(customRepsonseTime)) {
            try {
                responseTime = Integer.parseInt(customRepsonseTime);
                System.out.println("Custom responseTime = "+responseTime);
            } catch (NumberFormatException e) {
                System.err.printf("Invalid value for 'responseTime': '%s'%n", customRepsonseTime);
            }
        }

        int matrixSize = 10;
        final String customMatrixSize = request.getParameter("matrixSize");
        if (StringUtils.isNotBlank(customMatrixSize)) {
            try {
                matrixSize = Integer.parseInt(customMatrixSize);
                System.out.println("Custom matrixSize = "+matrixSize);
            } catch (NumberFormatException e) {
                System.err.printf("Invalid value for 'matrixSize': '%s'%n", customRepsonseTime);
            }
        }

        final long responseTimeMillis = responseTime * 1000L;
        BigDecimal determinantAccumulator = BigDecimal.ZERO;
        int iterations = 0;
        final Predicate<Object> whileCondition = new Predicate<Object>() {
            @Override
            public boolean apply(@NullableDecl Object unused) {
                return durationSince(startTime) < responseTimeMillis;
            }
        };
        final Predicate<Object> stopCondition = Predicates.not(whileCondition);
        for (; whileCondition.apply(null); iterations++) {
            try {
                final BigDecimal[][] matrix = generateMatrix(matrixSize, stopCondition);
                final BigDecimal determinant = computDeterminant(matrix, stopCondition);
                System.out.printf("det = %s%n", determinant.toString());
                determinantAccumulator = determinantAccumulator.add(determinant);
            } catch (ComputeTimeEndedException e) {
                System.out.println("Stopped computing determinants.");
                e.printStackTrace();
                break;
            }
        }
        // this is just to use the value so JIT doesn't remove any computation
        System.out.printf("Sum of computed determinants: %s (%d iterations)%n", determinantAccumulator.toString(), iterations);
    }

    private static class ComputeTimeEndedException extends Exception {
        public ComputeTimeEndedException(String message) {
            super(message);
        }
    }

    private static BigDecimal computDeterminant(BigDecimal[][] matrix, Predicate<?> stopCondition) throws ComputeTimeEndedException {
        assertSquareMatrix(matrix);
        final int size = matrix.length;
        if (size == 2) {
            return matrix[0][0].multiply(matrix[1][1]).subtract(matrix[0][1].multiply(matrix[1][0]));
        }
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < size; i++) {
            checkComputeTime(stopCondition, "computeDeterminant; size="+size+", i="+i);
            final boolean negative = i % 2 == 0;
            final BigDecimal cellI0 = matrix[i][0];
            final BigDecimal cofactor = computDeterminant(ithMinor(i, matrix, stopCondition), stopCondition);
            final BigDecimal ithTerm = cellI0.multiply(cofactor);
            if (negative) {
                result = result.subtract(ithTerm);
            } else {
                result = result.add(ithTerm);
            }
        }
        return result;
    }

    private static void checkComputeTime(Predicate<?> stopCondition, String exceptionMessage) throws ComputeTimeEndedException {
        if (stopCondition.apply(null)) {
            throw new ComputeTimeEndedException(exceptionMessage);
        }
    }

    private static <T> void assertSquareMatrix(T[][] matrix) {
        if (matrix.length != matrix[0].length) {
            throw new IllegalArgumentException(String.format("not a square matrix: %dx%d", matrix.length, matrix[0].length));
        }
    }

    private static BigDecimal[][] ithMinor(int row, BigDecimal[][] matrix, Predicate<?> stopCondition) throws ComputeTimeEndedException {
        assertSquareMatrix(matrix);
        final int size = matrix.length;
        if (size < 3) {
            throw new IllegalStateException("Expects at least a 3x3 matrix");
        }
        final int rsize = size - 1;
        final BigDecimal[][] result = new BigDecimal[rsize][rsize];
        int ri = 0;
        int rj = 0;
        for (int i = 0; i < size; i++) {
            checkComputeTime(stopCondition, "ithMinor; size="+size+", i="+i);
            if (i == row) {
                continue;
            }
            rj = 0;
            for (int j = 1; j < size; j++) {
                result[ri][rj] = matrix[i][j];
                rj++;
            }
            ri++;
        }
        return result;
    }

    private static final BigDecimal MATRIX_VALUE_RANGE = BigDecimal.valueOf(1_000_000);
    private static final ThreadLocalRandom rand = ThreadLocalRandom.current();
    /**
     * with values between 0 and 10^6
     * @param size
     * @param stopCondition
     * @return
     */
    private static BigDecimal[][] generateMatrix(int size, Predicate<Object> stopCondition) throws ComputeTimeEndedException {
        final BigDecimal[][] result = new BigDecimal[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                checkComputeTime(stopCondition, "generateMatrix; size="+size+", [i, j]=["+i+", "+j+"]");
                final BigDecimal nextFraction = BigDecimal.valueOf(rand.nextDouble());
                final BigDecimal cell = nextFraction.multiply(MATRIX_VALUE_RANGE);
                result[i][j] = cell;
            }
        }
        return result;
    }

    private static long durationSince(long startTime) {
        return System.currentTimeMillis() - startTime;
    }
}