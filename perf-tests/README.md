# Performance test for Application Insights

Go to the root folder of ApplicationInsights-Java repository.

Build tha Application Insights JAR: `./gradlew assemble`

Go to the perf-tests folder: `cd perf-tests`

Execute the performance tests: `./gradlew test`

A test measures the heap allocation at the start-up of a Spring Boot application with Application Insights enabled. This test saves the measure into an `allocation-at-startup.txt` file in the `build` folder. 
