package com;

import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import java.util.concurrent.Executor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

	@Bean
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new MyThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(2);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("AsyncTaskExecutor-");
		executor.initialize();
		return executor;
	}

	private final class MyThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

		@Override
		public void execute(Runnable command) {
			super.execute(new Wrapped(command, ThreadContext.getRequestTelemetryContext()));
		}
	}

	private final class Wrapped implements Runnable {
		private final Runnable task;
		private final RequestTelemetryContext rtc;

		Wrapped(Runnable task, RequestTelemetryContext rtc) {
			this.task = task;
			this.rtc = rtc;
		}

		@Override
		public void run() {
			if (ThreadContext.getRequestTelemetryContext() != null) {
				ThreadContext.remove();
			}
			System.out.println("*****Id is:" + rtc.getHttpRequestTelemetry().getId());
			ThreadContext.setRequestTelemetryContext(rtc);
			task.run();
		}
	}
}
