package com.microsoft.jfr;

import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.testng.Assert.*;

public class RecordingTest {

    FlightRecorderConnection flightRecorderConnection = null;

    @BeforeTest
    public void setup() {
        MBeanServerConnection mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            flightRecorderConnection = FlightRecorderConnection.connect(mBeanServer);
        } catch (InstanceNotFoundException e) {
            fail("Either JVM does not support JFR, or experimental options need to be enabled", e);
        } catch (IOException e) {
            // possible that this can be thrown, but should not happen in this context
            fail("IOException not expected", e);
        } catch (JfrStreamingException reallyBad) {
            fail ("something really bad happened", reallyBad);
        }
    }

    @AfterTest
    public void tearDown() {
        try {
            Path userDir = Paths.get(System.getProperty("user.dir"));
            Files.list(userDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".jfr"))
                    .forEach(jfrFile -> {
                        try {
                            Files.delete(jfrFile);
                        } catch (IOException ignored) {
                            Reporter.log(ignored.getMessage());
                        }
                    });
        } catch (IOException ignored) {
            Reporter.log(ignored.getMessage());
        }

    }

    @Test
    public void assertNewRecordingInitialValues() {
        assert flightRecorderConnection != null;
        Recording recording = flightRecorderConnection.newRecording(null, null);
        assertEquals(recording.getState(), Recording.State.NEW);
        assertEquals(recording.getId(), -1);
    }


    @Test
    public void assertRecordingStartIdAndState() {
        assert flightRecorderConnection != null;
        Recording recording = flightRecorderConnection.newRecording(null, null);
        try {
            long id = recording.start();
            assertEquals(recording.getId(), id);
            assertEquals(recording.getState(), Recording.State.RECORDING);
        } catch (IOException|IllegalStateException| JfrStreamingException e) {
            fail("assertRecordingStartIdAndState caught exception", e);
        }
    }

    @Test
    public void assertRecordingStopState() {
        assert flightRecorderConnection != null;
        Recording recording = flightRecorderConnection.newRecording(null, null);
        try {
            long id = recording.start();
            assertEquals(recording.getId(), id);
            recording.stop();
            assertEquals(recording.getState(), Recording.State.STOPPED);
        } catch (IOException|IllegalStateException| JfrStreamingException e) {
            fail("assertRecordingStopState caught exception", e);
        }
    }

    @Test
    public void assertRecordingCloseState() {
        assert flightRecorderConnection != null;
        Recording recording = flightRecorderConnection.newRecording(null, null);
        try {
            long id = recording.start();
            assertEquals(recording.getId(), id);
            recording.close();
            assertEquals(recording.getState(), Recording.State.CLOSED);
        } catch (IOException|IllegalStateException| JfrStreamingException e) {
            fail("assertRecordingCloseState caught exception", e);
        }
    }

    static void reflectivelyInvokeMethods(Recording recording, Object[] args) throws Exception {
        Class<Recording> clazz = (Class<Recording>) recording.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (int argc = 0; argc < args.length; ) {
            String methodName = (String) args[argc++];
            Method method = null;
            for (Method m : methods) {
                if (m.getName().equals(methodName)) {
                    if ("getStream".equals(m.getName())) {
                        if (m.getParameterTypes().length < 3) {
                            // Always pick getStream(Instant,Instant,long)
                            continue;
                        }
                    }
                    method = m;
                    break;
                }
            }
            if (method == null) {
                throw new NoSuchMethodException(methodName + " not found in declared methods of " + clazz.getName());
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object [] methodArgs = new Object[parameterTypes.length];
            int index = 0;
            for(Class<?> type : parameterTypes) {
                if ("boolean".equals(type.getName())) {
                    methodArgs[index++] = ((Boolean)args[argc++]).booleanValue();
                } else if ("long".equals(type.getName())) {
                    methodArgs[index++] = ((Long)args[argc++]).longValue();
                } else {
                    methodArgs[index++] = type.cast(args[argc++]);
                }
            }
            method.invoke(recording, methodArgs);
        }
    }

    @DataProvider(name="validStateChanges")
    public static Object[][] validStateChanges() {
        return new Object[][]{
                {"start"},
                {"start", "start"},
                {"stop"},
                {"stop", "stop"},
                {"close"},
                {"close", "close"},
                {"start", "stop"},
                {"start", "stop", "start"},
                {"start", "stop", "start", "close"},
                {"start", "close"},
                {"start", "stop", "close"},
                {"start", "clone", false},
                {"start", "stop", "clone", false},
                {"start", "stop", "getStream", null, null, 500000L},
                {"start", "dump", "test.jfr", "stop"},
                {"start", "stop", "dump", "test.jfr", "stop"},
                {"start", "stop", "dump", "test.jfr", "close"}
        };
    };

    @Test(dataProvider = "validStateChanges")
    public void assertValidStateChangeNoException(Object[] args) {
        assert flightRecorderConnection != null;
        Recording recording = flightRecorderConnection.newRecording(null, null);
        try {
            reflectivelyInvokeMethods(recording, args);
        } catch (Exception e) {
            if (e instanceof InvocationTargetException && e.getCause() instanceof IllegalStateException) {
                fail("IllegalStateException was not expected");
            }
            fail("Bad test code", e);
        }
    }

    @DataProvider(name="invalidStateChanges")
    public static Object[][] invalidStateChanges() {
        return new Object[][]{
                {"getStream", null, null, 500000L},
                {"dump", "test.jfr"},
                {"close", "start"},
                {"close", "stop"},
                {"start", "close", "stop"},
                {"start", "close", "clone", false},
                {"start", "close", "dump", "test.jfr"},
                {"start", "getStream", null, null, 500000L},
                {"start", "close", "getStream", null, null, 500000L}
        };
    };

    @Test(dataProvider = "invalidStateChanges", expectedExceptions = {IllegalStateException.class})
    public void assertInvalidStateChangeThrowsIllegalStateException(Object[] args) {
        assert flightRecorderConnection != null;
        Recording recording = flightRecorderConnection.newRecording(null, null);
        try {
            reflectivelyInvokeMethods(recording, args);
        } catch (Exception e) {
            if (e instanceof InvocationTargetException && e.getCause() instanceof IllegalStateException) {
                throw (IllegalStateException)e.getCause();
            }
            fail("Bad test code", e);
        }
    }

    @DataProvider(name="options")
    public static Object[][] options() {
        return new Object[][] {
                {""},
                {"name=test"},
                {"maxAge=30 s", "disk=true"},
                {"maxSize=1048576","disk=true"},
                {"dumpOnExit=true"},
                {"destination=temp.jfr","disk=true"},
                {"duration=30 s"},
                {"name=test", "maxAge=30 s", "maxSize=1048576","dumpOnExit=true","destination=temp.jfr","disk=true","duration=30 s"},
        };
    }

    @Test(dataProvider = "options")
    public void assertRecordingOptionsAreSetInFlightRecorderMXBean(String[] options) {
        try {
            MBeanServerConnection mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName flightRecorder = new ObjectName("jdk.management.jfr:type=FlightRecorder");
            ObjectInstance objectInstance = mBeanServer.getObjectInstance(flightRecorder);
            RecordingOptions.Builder builder = new RecordingOptions.Builder();
            for(String opt : options) {
                String[] keyValue = opt.split("=");
                if (keyValue.length < 2) continue;
                String key = keyValue[0];
                String value = keyValue[1];
                Method method = RecordingOptions.Builder.class.getMethod(key, String.class);
                method.invoke(builder, value);
            }
            RecordingOptions recordingOptions = builder.build();
            Recording recording = flightRecorderConnection.newRecording(recordingOptions, null);
            long id = recording.start();
            TabularData flightRecorderMXBeanOptions =
                    (TabularData)mBeanServer.invoke(flightRecorder, "getRecordingOptions", new Object[]{id}, new String[]{long.class.getName()});
            ((Collection<CompositeData>)flightRecorderMXBeanOptions.values())
                    .forEach(compositeData -> {
                        String key = (String)compositeData.get("key");
                        String getter = "get" + key.substring(0,1).toUpperCase(Locale.ROOT) + key.substring(1);
                        String expected = (String)compositeData.get("value");
                        try {
                            Method method = RecordingOptions.class.getMethod(getter);
                            String actual = (String) method.invoke(recordingOptions);
                            // special case for name since FlightRecorderMXBean returns id as default
                            // and for destination since FlightRecorderMXBean returns null as default
                            if (!("name".equals(key) && "".equals(actual))
                                && !("destination".equals(key) && "".equals(actual))) {
                                assertEquals(actual, expected, getter);
                            }
                        } catch (NoSuchMethodException|IllegalArgumentException|IllegalAccessException|InvocationTargetException badAPI) {
                            fail("Issue in RecordingOptions API: " + badAPI.getMessage());
                        }
                    }
            );
            recording.stop();
            recording.close();
        } catch (NoSuchMethodException|IllegalArgumentException badData) {
            fail("Issue in test data: " + badData.getMessage());
        } catch (InvocationTargetException|IllegalAccessException badCode) {
            fail("Issue in code: " + badCode);
        } catch (IOException ioe) {
            // possible that this can be thrown, but should not happen in this context
            fail("IOException not expected: ", ioe);
        } catch (JfrStreamingException | ReflectionException| MBeanException badBean) {
            fail("Error thrown by MBean server or FlightRecorderMXBean: ", badBean);
        } catch (MalformedObjectNameException badTest) {
            fail("Error internal to the test: ", badTest);
        } catch (InstanceNotFoundException badJvm) {
            fail("Either JVM does not support JFR, or experimental options need to be enabled");
        } catch (RuntimeMBeanException ex) {
            // some versions of java don't support the 'destination' option
            if (!(ex.getCause() instanceof IllegalArgumentException)) {
                fail("Something bad happened", ex);
            }
        }
    }

    // something to do
    private static void fib(int limit) {
        BigDecimal[] fibs = new BigDecimal[limit];
        fibs[0] =  new BigDecimal(0);
        fibs[1] = new BigDecimal(1);
        for(int i=2; i<fibs.length; i++) {
            fibs[i] = fibs[i-1].add(fibs[i-2]);
        }
    }

    @Test
    public void assertFileExistsAfterRecordingDump() {
        Path dumpFile = null;
        try {
            dumpFile = Paths.get(System.getProperty("user.dir"),"testRecordingDump_dumped.jfr");
            Files.deleteIfExists(dumpFile);

            RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
            Recording recording = flightRecorderConnection.newRecording(recordingOptions, null);
            long id = recording.start();
            Instant now = Instant.now();
            Instant then = now.plusSeconds(1);
            while (Instant.now().compareTo(then) < 0) {
                fib(Short.MAX_VALUE); // do something
            }
            recording.stop();
            recording.dump(dumpFile.toString());
            assertTrue(Files.exists(dumpFile));
        } catch (IllegalArgumentException badData) {
            fail("Issue in test data: " + badData.getMessage());
        } catch (IOException ioe) {
            // possible that this can be thrown, but should not happen in this context
            fail("IOException not expected: ", ioe);
        } catch (JfrStreamingException badBean) {
            fail("Error thrown by MBean server or FlightRecorderMXBean: ", badBean);
        } finally {
            if (dumpFile != null) {
                try {
                    Files.deleteIfExists(dumpFile);
                } catch (IOException ignore) {
                }
            }
        }
    }

    @Test
    public void assertFileExistsAfterRecordingStream() {
        Path streamedFile = null;
        try {
            streamedFile = Paths.get(System.getProperty("user.dir"),"testRecordingStream_getStream.jfr");
            Files.deleteIfExists(streamedFile);

            RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
            Recording recording = flightRecorderConnection.newRecording(recordingOptions, null);
            long id = recording.start();
            Instant now = Instant.now();
            Instant then = now.plusSeconds(1);
            while (Instant.now().compareTo(then) < 0) {
                fib(Short.MAX_VALUE); // do something
            }
            recording.stop();

            try (InputStream inputStream = recording.getStream(now, then); // get the whole thing.
                 OutputStream outputStream = new FileOutputStream(streamedFile.toFile())) {
                int c = -1;
                while ((c = inputStream.read()) != -1) outputStream.write(c);
            } catch (IOException e) {
                fail(e.getMessage(), e);
            }

            assertTrue(Files.exists(streamedFile));

        } catch (IllegalArgumentException badData) {
            fail("Issue in test data: " + badData.getMessage());
        } catch (IOException ioe) {
            // possible that this can be thrown, but should not happen in this context
            fail("IOException not expected: ", ioe);
        } catch (JfrStreamingException badBean) {
            fail("Error thrown by MBean server or FlightRecorderMXBean: ", badBean);
        } finally {
            if (streamedFile != null) {
                try {
                    Files.deleteIfExists(streamedFile);
                } catch (IOException ignore) {
                }
            }
        }
    }

    @Test
    public void assertStreamedFileEqualsDumpedFile() {
        Path streamedFile = null;
        Path dumpedFile = null;
        try {
            streamedFile = Paths.get(System.getProperty("user.dir"),"testRecordingStream_getStream.jfr");
            dumpedFile = Paths.get(System.getProperty("user.dir"),"testRecordingStream_dumped.jfr");
            Files.deleteIfExists(streamedFile);

            RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
            Recording recording = flightRecorderConnection.newRecording(recordingOptions, null);
            long id = recording.start();
            Instant now = Instant.now();
            Instant then = now.plusSeconds(1);
            while (Instant.now().compareTo(then) < 0) {
                fib(Short.MAX_VALUE); // do something
            }
            recording.stop();
            recording.dump(dumpedFile.toString());
            try (InputStream inputStream = recording.getStream(now, then); // get the whole thing.
                 OutputStream outputStream = new FileOutputStream(streamedFile.toFile())) {
                int c = -1;
                while ((c = inputStream.read()) != -1) outputStream.write(c);
            } catch (IOException e) {
                fail(e.getMessage(), e);
            }

            try (InputStream streamed = new FileInputStream(streamedFile.toFile());
                 InputStream dumped = new FileInputStream(dumpedFile.toFile())) {
                int a = -1;
                int b = -1;
                do {
                    a = streamed.read();
                    b = dumped.read();
                } while (a != -1 && b != -1 && a == b);
                if (a != b) fail(dumpedFile + " differs from " + streamedFile);
            } catch (IOException e) {
                fail(e.getMessage(), e);
            }
            // if we get here, then the files compare the same and there is no need to save them
            Files.deleteIfExists(dumpedFile);
            Files.deleteIfExists(streamedFile);
        } catch (IllegalArgumentException badData) {
            fail("Issue in test data: " + badData.getMessage());
        } catch (IOException ioe) {
            // possible that this can be thrown, but should not happen in this context
            fail("IOException not expected: ", ioe);
        } catch (JfrStreamingException badBean) {
            fail("Error thrown by MBean server or FlightRecorderMXBean: ", badBean);
        }
    }

    @Test
    public void assertRecordingCloneState() {
        // Recording#clone returns a clone of the recording with the same state, but clone has its own id.
        // Recording#clone with 'true' causes clone to close before returning.
        try {

            RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
            Recording recording = flightRecorderConnection.newRecording(recordingOptions, null);
            long id = recording.start();
            Recording clone = recording.clone(true);
            assertTrue(recording.getState() == Recording.State.RECORDING);
            assertTrue(clone.getState() == Recording.State.STOPPED);
            assertTrue(recording.getId() != clone.getId());
            recording.stop();
            recording.close();
        } catch (IOException ioe) {
            // possible that this can be thrown, but should not happen in this context
            fail("IOException not expected: ", ioe);
        } catch (JfrStreamingException badBean) {
            fail("Error thrown by MBean server or FlightRecorderMXBean: ", badBean);
        }
    }

}
