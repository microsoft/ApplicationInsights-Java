// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.quickperf.junit5.QuickPerfTest;
import org.quickperf.jvm.annotations.JvmOptions;
import org.quickperf.jvm.annotations.MeasureHeapAllocation;
import org.quickperf.writer.WriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.samples.petclinic.PetClinicApplication;

@QuickPerfTest
class AiStartUpTest {
  private static final Logger LOG = LoggerFactory.getLogger(AiStartUpTest.class);

  @Test
  @Order(1)
  @JvmOptions("-javaagent:" + "build/applicationinsights-agent.jar")
  @MeasureHeapAllocation(writerFactory = FileWriterFactory.class, format = "%s")
  void heap_allocation_main_thread_spring_boot() {
    PetClinicApplication.main(new String[] {});
  }

  public static class FileWriterFactory implements WriterFactory {
    @Override
    public Writer buildWriter() throws IOException {
      return new FileWriter("build/allocation-at-startup.txt");
    }
  }

  @Test
  @Order(2)
  void sendResults() throws IOException {
    String formattedAllocation = Files.readString(Path.of("build/allocation-at-startup.txt"));

    LOG.info(formattedAllocation);

    String allocationValue = formattedAllocation.split(" Mega bytes")[0];

    Calendar calendar = Calendar.getInstance();

    Date date = calendar.getTime();

    PerfResultSender.send(
        date, "SDKPerfJavaAllocAtStartUp", "AllocationStartUpJava", allocationValue, "MB");
  }
}
