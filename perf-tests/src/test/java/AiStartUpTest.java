import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import org.junit.jupiter.api.Test;
import org.quickperf.junit5.QuickPerfTest;
import org.quickperf.jvm.annotations.JvmOptions;
import org.quickperf.jvm.annotations.MeasureHeapAllocation;
import org.quickperf.writer.WriterFactory;
import org.springframework.samples.petclinic.PetClinicApplication;

@QuickPerfTest
class AiStartUpTest {

  @Test
  @JvmOptions("-javaagent:" + "build/applicationinsights-agent.jar")
  @MeasureHeapAllocation(writerFactory = FileWriterFactory.class, format = "%s")
  void heap_allocation_main_thread_spring_boot() {
    PetClinicApplication.main(new String[] {});
  }

   public static class FileWriterFactory implements WriterFactory  {

    @Override
    public Writer buildWriter() throws IOException {
      return new FileWriter("build/allocation-at-startup.txt");
    }
  }

}
