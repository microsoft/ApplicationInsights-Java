package com.microsoft.applicationinsights.channel.contracts;
/**
 * Enum DependencyKind.
 */
public class DependencyKind
{
    public static int Undefined = 0;
    public static int HttpOnly = 1;
    public static int HttpAny = 2;
    public static int SQL = 3;
}
