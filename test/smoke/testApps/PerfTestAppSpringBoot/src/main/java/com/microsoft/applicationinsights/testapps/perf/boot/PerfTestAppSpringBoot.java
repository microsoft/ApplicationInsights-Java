package com.microsoft.applicationinsights.testapps.perf.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
public class PerfTestAppSpringBoot extends SpringBootServletInitializer {
        @Override
        protected SpringApplicationBuilder configure(SpringApplicationBuilder applicationBuilder) {
            return applicationBuilder.sources(PerfTestAppSpringBoot.class);
        }

        public static void main(String[] args) {
            SpringApplication.run(PerfTestAppSpringBoot.class, args);
        }
}
