package com.microsoft.ajl.simple;

import io.opentelemetry.api.trace.Span;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public String root() {
        return "OK";
    }


    @GetMapping("/test")
    public String test() {
        Span.current().setAttribute("myattr1", "myvalue1");
        Span.current().setAttribute("myattr2", "myvalue2");
        Span.current().setAttribute("enduser.id", "myuser");
        Span.current().updateName("myspanname");
        return "OK!";
    }
}
