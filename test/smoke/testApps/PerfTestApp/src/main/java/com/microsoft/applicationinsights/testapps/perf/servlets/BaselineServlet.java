package com.microsoft.applicationinsights.testapps.perf.servlets;

import com.microsoft.applicationinsights.testapps.perf.TestCaseRunnable;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet({"/baseline"})
public class BaselineServlet extends APerfTestServlet {

    @Override
    protected void reallyDoGet(HttpServletRequest req, HttpServletResponse resp) {
        new TestCaseRunnable(new Runnable() {
            @Override
            public void run() {
                // nop
            }
        }).run();
    }
}