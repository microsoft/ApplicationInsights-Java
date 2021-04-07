package com.microsoft.applicationinsights.smoketestapp;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.extension.annotations.WithSpan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public String root() {
        return "OK";
    }

    @GetMapping("/test-api")
    public String testApi() {
        Span.current().setAttribute("myattr1", "myvalue1");
        Span.current().setAttribute("myattr2", "myvalue2");
        Span.current().setAttribute("enduser.id", "myuser");
        Span.current().updateName("myspanname");
        return "OK!";
    }

    @GetMapping("/test-annotations")
    public String testAnnotations() {
        return underAnnotation();
    }

    @WithSpan
    private String underAnnotation() {
        return "OK!";
    }
}
