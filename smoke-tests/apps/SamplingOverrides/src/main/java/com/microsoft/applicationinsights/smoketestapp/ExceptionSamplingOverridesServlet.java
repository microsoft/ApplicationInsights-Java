// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/trackException")
public class ExceptionSamplingOverridesServlet extends HttpServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    throw new RuntimeException("this is an expected exception");
  }
}
