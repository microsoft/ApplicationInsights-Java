package com.microsoft.ajl.simplecalc;

import javax.servlet.ServletException;
import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import redis.clients.jedis.Jedis;

/**
 * Servlet implementation class SimpleTrackDependencyAndExceptionServlet
 */
@WebServlet(description = "Performs given calculation", urlPatterns = { "/trackData" })
public class SimpleTrackDependencyAndExceptionServlet extends HttpServlet {
    private static final long serialVersionUID = -7496476539225639976L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.getRrenderHtml(request, response);

        try {
            String redisHostname = System.getenv("REDIS");
            Jedis jedis = new Jedis(redisHostname, 6379);
            jedis.set("foo", "bar");
        } catch (Exception e) {
            //TODO: handle exception
            e.printStackTrace();
        }

    }
}