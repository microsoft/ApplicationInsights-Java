/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

#include "etw_provider.h"
#include <VersionHelpers.h>

#ifndef NDEBUG
#   include <stdio.h>
#   define DBG(...) { printf(__VA_ARGS__); }
#elif
#   define DBG(...) do { } while(0)
#endif

#define EVENT_KEYWORDS_ALL 0x0

TRACELOGGING_DEFINE_PROVIDER(
    provider_EtwHandle,
    "Microsoft-ApplicationInsights-Java-IPA",
    // {1f0dc33f-30ae-5ff3-8b01-8ca9b8509233}
    (0x1f0dc33f,0x30ae,0x5ff3,0x8b,0x01,0x8c,0xa9,0xb8,0x50,0x92,0x33));

JNIEXPORT void JNICALL Java_com_microsoft_applicationinsights_internal_etw_EtwProvider_cppWriteEvent(JNIEnv * env, jobject objJavaThis, 
    jint jiEventId, jstring jstrEventName, jint jiLevel, jstring jstrExtensionVersion, jstring jstrSubscriptionId, jstring jstrAppName, jstring jstrLogger, jstring jstrMessage) {
        // TODO is eventId needed?
        
        jboolean copy = JNI_FALSE;
        // convert all jstrings
        const char* eventName = (*env)->GetStringUTFChars(jstrEventName, &copy);
        const char* extensionVersion = (*env)->GetStringUTFChars(jstrExtensionVersion, &copy);
        const char* subscriptionId = (*env)->GetStringUTFChars(jstrSubscriptionId, &copy);
        const char* appName = (*env)->GetStringUTFChars(jstrAppName, &copy);
        const char* logger = (*env)->GetStringUTFChars(jstrLogger, &copy);
        const char* message = (*env)->GetStringUTFChars(jstrMessage, &copy);

        // log debug message
        DBG("WriteEvent, eventId=%d, eventName='%s', level=%u, extVer='%s', subId='%s', appName='%s', logger='%s', message='%s'", jiEventId, eventName, jiLevel, extensionVersion, subscriptionId, appName, logger, message);

        // write message
        if (IsWindows10OrGreater()) {
            TraceLoggingWrite(
                provider_EtwHandle,
                eventName,
                TraceLoggingChannel(WINEVENT_CHANNEL_TRACELOGGING),
                TraceLoggingLevel(jiLevel),
                TraceLoggingValue(message, "msg")
                TraceLoggingValue(extensionVersion, "ExtVer"),
                TraceLoggingValue(subscriptionId, "SubscriptionId"),
                TraceLoggingValue(appName, "AppName"),
                TraceLoggingValue(logger, "Logger")
            );
        } else {
            TraceLoggingWrite(
                provider_EtwHandle,
                eventName,
                TraceLoggingLevel(jiLevel),
                TraceLoggingValue(message, "msg")
                TraceLoggingValue(extensionVersion, "ExtVer"),
                TraceLoggingValue(subscriptionId, "SubscriptionId"),
                TraceLoggingValue(appName, "AppName"),
                TraceLoggingValue(logger, "Logger")
            );
        }

        // release strings
        (env*)->ReleaseStringUTFChars(jstrEventName, eventName);
        (env*)->ReleaseStringUTFChars(jstrExtensionVersion, extensionVersion);
        (env*)->ReleaseStringUTFChars(jstrSubscriptionId, subscriptionId);
        (env*)->ReleaseStringUTFChars(jstrAppName, appName);
        (env*)->ReleaseStringUTFChars(jstrLogger, logger);
        (env*)->ReleaseStringUTFChars(jstrMessage, message);
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_applicationinsights_internal_etw_EtwProvider_cppIsProviderEnabled(JNIEnv * env, jobject objJavaThis, 
    jint level) {
        // jint<signed 32 bits> -> UCHAR<unsigned char/8 bits>
        BOOLEAN enabled = TraceLoggingProviderEnabled(provider_EtwHandle, level, EVENT_KEYWORD_FILTER_ALL);

        DBG("TraceLoggingProviderEnabled, level=%u: %u\n", level, enabled);

        return enabled;
}