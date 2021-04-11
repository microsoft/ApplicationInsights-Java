package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/*")
public class SimpleServlet extends HttpServlet {

    public SimpleServlet() {
        System.out.println("hi");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("text/html;charset=UTF-8");

        PrintWriter writer = response.getWriter();
        writer.println("<html>");
        writer.println("<head><title>Web With Auto Registration</title></head>");
        writer.println("<body>");
        writer.println("<p>Hello World!</p>");
        writer.println("</body></html>");
    }
}
