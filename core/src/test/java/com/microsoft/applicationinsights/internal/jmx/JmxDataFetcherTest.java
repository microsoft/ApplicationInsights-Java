package com.microsoft.applicationinsights.internal.jmx;

import org.junit.*;

import javax.management.*;
import java.beans.ConstructorProperties;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class JmxDataFetcherTest {


    protected static final int POINT_X = 111;
    protected static final int POINT_Y = 222;
    protected static final int LINE_START_X = 333;
    protected static final int LINE_START_Y = 444;
    protected static final int LINE_FINISH_X = 555;
    protected static final int LINE_FINISH_Y = 666;

    public interface StubMXBean {
        public int getIntSample();
        public double getDoubleSample();
        public long getLongSample();
    }

    public static class TestStub implements StubMXBean {
        int i;
        double d;
        long l;

        TestStub(int i, double d, long l) {
            this.i = i;
            this.d = d;
            this.l = l;
        }

        @Override
        public int getIntSample() {
            return i;
        }

        @Override
        public double getDoubleSample() {
            return d;
        }

        @Override
        public long getLongSample() {
            return l;
        }
    }

    public static class TestPoint {
        private final int x;
        private final int y;

        @ConstructorProperties({"x", "y"})
        public TestPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    public static class TestLine {
        private final PointHolderMXBean start;
        private final PointHolderMXBean finish;

        @ConstructorProperties({"start", "finish"})
        public TestLine(PointHolderMXBean start, PointHolderMXBean finish) {
            this.start = start;
            this.finish = finish;
        }

        public PointHolderMXBean getStart() {
            return start;
        }

        public PointHolderMXBean getFinish() {
            return finish;
        }
    }

    /**
     * for testing composite
     */
    public interface PointHolderMXBean {
        TestPoint getPoint();
    }

    public static class PointHolderImpl implements PointHolderMXBean {

        private TestPoint point;

        @Override
        public TestPoint getPoint() {
            return this.point;
        }

        public void setPoint(TestPoint point) {
            this.point = point;
        }
    }

    /**
     * for testing tabular
     */
    public interface PointMapMXBean {
        Map<String, TestPoint> getPoints();
    }

    public static class PointMapImpl implements PointMapMXBean {

        private Map<String, TestPoint> points = new HashMap<>();
        private int length;

        @Override
        public Map<String, TestPoint> getPoints() {
            return points;
        }

        public void addPoint(String color, TestPoint point) {
            points.put(color, point);
        }
    }


    private static final String TESTSTUB_FQN = "JSDKTests:type=TestStub";
    private static final String POINT_HOLDER_FQN = "JSDKTests:type=PointHolderImpl";
    private static final String MAP_FQN = "JSDKTests:type=PointMapImpl";
    private static final int TEST_INT = 123;
    private static final double TEST_DOUBLE = 243.5557;
    private static final long TEST_LONG = 789789789000001L;

    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    private ObjectName testStubBeanName;
    private ObjectName pointBeanName;
    private ObjectName mapBeanName;

    private TestStub testStub;
    private PointHolderImpl testPoint;
    private PointMapImpl testMap;


    @Before
    public void setup() throws Exception {
        testStubBeanName = new ObjectName(TESTSTUB_FQN);
        pointBeanName = new ObjectName(POINT_HOLDER_FQN);
        mapBeanName = new ObjectName(MAP_FQN);

        testStub = new TestStub(TEST_INT, TEST_DOUBLE, TEST_LONG);
        server.registerMBean(testStub, testStubBeanName);

        testPoint = new PointHolderImpl();
        testPoint.setPoint(new TestPoint(POINT_X, POINT_Y));
        server.registerMBean(testPoint, pointBeanName);

        testMap = new PointMapImpl();
        testMap.addPoint("start", new TestPoint(LINE_START_X, LINE_START_Y));
        testMap.addPoint("finish", new TestPoint(LINE_FINISH_X, LINE_FINISH_Y));
        server.registerMBean(testMap, mapBeanName);
    }

    @After
    public void tearDown() throws Exception {
        server.unregisterMBean(testStubBeanName);
        server.unregisterMBean(pointBeanName);
        server.unregisterMBean(mapBeanName);
    }

    @Test(expected = Exception.class)
    public void testBadAttributeName() throws Exception {
        List<JmxAttributeData> attributes = new ArrayList<JmxAttributeData>();
        attributes.add(new JmxAttributeData("Int", "WrongNameIntSample"));
        attributes.add(new JmxAttributeData("Double", "WrongNameDoubleSample"));
        attributes.add(new JmxAttributeData("Long", "WrongNameLongSample"));
        JmxDataFetcher.fetch(TESTSTUB_FQN, attributes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadName() throws Exception {
        List<JmxAttributeData> attributes = new ArrayList<JmxAttributeData>();
        attributes.add(new JmxAttributeData("Int", "IntSample"));
        JmxDataFetcher.fetch("JSDKTests:type=TestStub123", attributes);
    }

    @Test
    public void testWithChange() throws Exception {
        List<JmxAttributeData> attributes = new ArrayList<JmxAttributeData>();
        attributes.add(new JmxAttributeData("Int", "IntSample"));
        attributes.add(new JmxAttributeData("Double", "DoubleSample"));
        attributes.add(new JmxAttributeData("Long", "LongSample"));

        performTest(attributes, TEST_INT, TEST_DOUBLE, TEST_LONG);

        testStub.i = 1000;
        testStub.d = 2000.0;
        testStub.l = 3000L;

        performTest(attributes, 1000.0, 2000.0, 3000.0);
    }

    @Test
    public void testWithComposite() throws Exception {
        List<JmxAttributeData> attributes = new ArrayList<>();
        attributes.add(new JmxAttributeData("X Coord", "Point.x", "COMPOSITE"));
        attributes.add(new JmxAttributeData("Y Coord", "Point.y", "COMPOSITE"));

        final Map<String, Collection<Object>> result = JmxDataFetcher.fetch(POINT_HOLDER_FQN, attributes);
        assertThat(result.keySet(), hasItem("X Coord"));
        assertThat(result.keySet(), hasItem("Y Coord"));
        assertThat(result.get("X Coord"), hasSize(1));
        assertThat(result.get("X Coord"), hasItem(Integer.valueOf(POINT_X)));
        assertThat(result.get("Y Coord"), hasSize(1));
        assertThat(result.get("Y Coord"), hasItem(Integer.valueOf(POINT_Y)));
    }

    @Test
    public void testFetchRegularObject() throws Exception {
        ObjectName bean2 = new ObjectName(TESTSTUB_FQN +"123");
        final int testInt2 = 10101;
        final double testDouble2 = 20202.0202;
        final long testLong2 = 303030303L;

        server.registerMBean(new TestStub(testInt2, testDouble2, testLong2), bean2);
        Set<ObjectName> names = new HashSet<>();
        names.add(testStubBeanName);
        names.add(bean2);
        List<Object> result = new ArrayList<>();

        JmxDataFetcher.fetchRegularObjects(server, names, "IntSample", result);
        assertThat(result, hasSize(2));
        assertThat(result, hasItem(Integer.valueOf(TEST_INT)));
        assertThat(result, hasItem(Integer.valueOf(testInt2)));

        JmxDataFetcher.fetchRegularObjects(server, names, "DoubleSample", result);
        assertThat(result, hasSize(4));
        assertThat(result, hasItem(Integer.valueOf(TEST_INT)));
        assertThat(result, hasItem(Integer.valueOf(testInt2)));
        assertThat(result, hasItem(Double.valueOf(TEST_DOUBLE)));
        assertThat(result, hasItem(Double.valueOf(testDouble2)));

        JmxDataFetcher.fetchRegularObjects(server, names, "LongSample", result);
        assertThat(result, hasSize(6));
        assertThat(result, hasItem(Integer.valueOf(TEST_INT)));
        assertThat(result, hasItem(Integer.valueOf(testInt2)));
        assertThat(result, hasItem(Double.valueOf(TEST_DOUBLE)));
        assertThat(result, hasItem(Double.valueOf(testDouble2)));
        assertThat(result, hasItem(Long.valueOf(TEST_LONG)));
        assertThat(result, hasItem(Long.valueOf(testLong2)));
    }

    @Test
    public void testFetchCompositeObject() throws Exception {
        Set<ObjectName> names = new HashSet<>();
        names.add(pointBeanName);
        List<Object> result = new ArrayList<>();

        JmxDataFetcher.fetchCompositeObjects(server, names, "Point.x", result);
        assertThat(result, hasSize(1));
        assertThat(result, hasItem(Integer.valueOf(POINT_X)));
    }

    @Ignore // FIXME correctly construct a bean with tabular data... I can't find any examples; maybe i can find one in jconsole and look at it's impl
    @Test
    public void testFetchTabularObject() throws Exception {
        Set<ObjectName> names = new HashSet<>();
        names.add(mapBeanName);
        List<Object> result = new ArrayList<>();
        JmxDataFetcher.fetchTabularObjects(server, names, "Points.finish.y", result);
        assertThat(result, hasSize(1));
        assertThat(result, hasItem(Integer.valueOf(LINE_FINISH_Y)));
    }

    private static void performTest(
            List<JmxAttributeData> attributes,
            double expectedInt,
            double expectedDouble,
            double expectedLong) throws Exception {
        Map<String, Collection<Object>> result = JmxDataFetcher.fetch(TESTSTUB_FQN, attributes);

        assertNotNull(result);
        assertEquals(3, result.size());

        verify(result, "Int", expectedInt);
        verify(result, "Double", expectedDouble);
        verify(result, "Long", expectedLong);
    }

    private static void verify(Map<String, Collection<Object>> result, String key, double expectedValue) {
        Collection<Object> objects = result.get(key);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        double value = 0.0;
        for (Object obj : objects) {
            try {
                value += Double.parseDouble(String.valueOf(obj));
            } catch (Exception e) {
                Assert.fail("Exception thrown: "+e);
            }
        }

        assertEquals(expectedValue, value, Math.ulp(expectedValue));
    }
}