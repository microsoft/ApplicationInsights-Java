package com.microsoft.applicationinsights.testapps.perf.boot.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ViewController {

    @RequestMapping({"/", "/index.jsp"})
    public ModelAndView viewIndex() {
        return new ModelAndView("index");
    }
}
