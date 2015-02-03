package com.microsoft.applicationinsights.web.utils;

import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import java.lang.reflect.Field;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.HttpInputOverHTTP;
import org.eclipse.jetty.server.Request;
import com.microsoft.applicationinsights.web.internal.WebModulesContainer;

import static org.mockito.Mockito.mock;

/**
 * Created by yonisha on 2/3/2015.
 */
public class ServletUtils {

    private ServletUtils() {
    }

    public static WebModulesContainer setMockWebModulesContainer(Filter filter) {
        WebModulesContainer container = mock(WebModulesContainer.class);

        Field field = null;
        try {
            field = getFilterWebModulesContainersField(filter);
            field.set(filter, container);
        } catch (Exception e) {
            container = null;
        }

        return container;
    }

    public static WebModulesContainer getWebModuleContainer(Filter filter) {
        WebModulesContainer container = null;

        try {
            Field field = getFilterWebModulesContainersField(filter);
            container = (WebModulesContainer)field.get(filter);
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
        }

        return container;
    }

    public static ServletRequest generateDummyServletRequest() {
        return new Request(
                HttpChannel.getCurrentHttpChannel(),
                new HttpInputOverHTTP(HttpConnection.getCurrentConnection()));
    }

    // region Private

    private static Field getFilterWebModulesContainersField(Filter filter) throws NoSuchFieldException {
        Field field = filter.getClass().getDeclaredField("webModulesContainer");
        field.setAccessible(true);

        return field;
    }

    // endregion Private
}
