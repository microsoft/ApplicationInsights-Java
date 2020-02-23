package com.microsoft.applicationinsights.smoketestapp;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.lettuce.core.RedisClient;

@WebServlet("/*")
public class LettuceTestServlet extends HttpServlet {

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        try {
            doGetInternal(req);
            resp.getWriter().println("ok");
        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void doGetInternal(HttpServletRequest req) throws Exception {
        String pathInfo = req.getPathInfo();
        if (pathInfo.equals("/lettuce")) {
            lettuce();
        } else if (!pathInfo.equals("/")) {
            throw new ServletException("Unexpected url: " + pathInfo);
        }
    }

    private void lettuce() throws Exception {
        String hostname = System.getenv("REDIS");
        RedisClient redisClient = RedisClient.create("redis://" + hostname);
        redisClient.connect().sync();
        redisClient.shutdown();
    }
}
