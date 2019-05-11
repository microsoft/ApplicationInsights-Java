package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringBootApp extends SpringBootServletInitializer {

	public SpringBootApp() {
		super();

		// This lets tomcat handle error and hence filter catches exception.
		// Disables Springboot error handling which prevents response from propagating up.
		// See: https://github.com/spring-projects/spring-boot/commit/6381a07c71310c56dc29cf99709adf5fe6e6406a
		setRegisterErrorPageFilter(false);
	}
	@Override
	protected  SpringApplicationBuilder configure(SpringApplicationBuilder applicationBuilder) {
		return applicationBuilder.sources(SpringBootApp.class);
	}
  public static void main(String[] args) {
	  SpringApplication.run(SpringBootApp.class, args);
  }
}
