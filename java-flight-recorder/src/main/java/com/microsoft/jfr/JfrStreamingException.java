package com.microsoft.jfr;

import javax.management.JMException;

/**
 * An JfrStreamingException is a wrapper around specific {@code javax.management.JMException}
 * instances which might be thrown, either directly or indirectly, by methods of this package.
 * Exceptions of this type are not expected from a well behaved JVM.
 *
 * The cause of an {@code JfrStreamingException} will be one of the following:
 * <dl>
 *     <dt><em>javax.management.InstanceNotFoundException</em></dt>
 *     <dd>The FlightRecorderMXBean is not found on the MBean server. This could happen
 *     if the target JVM does not support Java Flight Recorder, or if experimental features
 *     need to be enabled on the target JVM. An InstanceNotFoundException is not expected after
 *     the connection is made to the FlightRecorderMXBean.
 *     </dd>
 *     <dt><em>javax.management.MBeanException</em></dt>
 *     <dd>Represents "user defined" exceptions thrown by MBean methods in the agent. It "wraps"
 *     the actual "user defined" exception thrown. This exception will be built by the MBeanServer
 *     when a call to an MBean method results in an unknown exception.</dd>
 *     <dt><em>javax.management.ReflectionException</em></dt>
 *     <dd>Represents exceptions thrown in the MBean server when using the java.lang.reflect
 *     classes to invoke methods on MBeans. It "wraps" the actual java.lang.Exception thrown.</dd>
 *     <dt><em>javax.management.MalformedObjectNameException</em></dt>
 *     <dd>The format of the string does not correspond to a valid ObjectName. This cause indicates
 *     a bug in the com.microsoft.censum.jfr package code.</dd>
 *     <dt><em>javax.management.openmbean.OpenDataException</em></dt>
 *     <dd>This exception is thrown when an open type, an open data or an open MBean
 *     metadata info instance could not be constructed because one or more validity constraints
 *     were not met. This cause indicates a bug in the com.microsoft.censum.jfr package code.</dd>
 * </dl>
 */
public class JfrStreamingException extends Exception {

    JfrStreamingException(String message, JMException cause) {
        super(message, cause);
    }

}
