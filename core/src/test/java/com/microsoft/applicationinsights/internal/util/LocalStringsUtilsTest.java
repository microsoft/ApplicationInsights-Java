package Core.src.test.java.com.microsoft.applicationinsights.internal.util;

import static org.junit.Assert.*;

import org.junit.Test;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;

public class LocalStringsUtilsTest {

	@Test
	public void test() {
		assertNotNull(LocalStringsUtils.getDateFormatter());
	}

}
