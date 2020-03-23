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
#include <winmeta.h>
#include <string>
#include <errno.h>

#ifndef NDEBUG
#   include <stdio.h>
#   define DBG(...) { printf(__VA_ARGS__); }
#elif
#   define DBG(...) do { } while(0)
#endif

#define EVENT_KEYWORD_FILTER_ALL 0x0

#define PROVIDER_HANDLE_VAR provider_EtwHandle

TRACELOGGING_DEFINE_PROVIDER(
    PROVIDER_HANDLE_VAR,
    "Microsoft-ApplicationInsights-Java-IPA",
    // {1f0dc33f-30ae-5ff3-8b01-8ca9b8509233}
    (0x1f0dc33f,0x30ae,0x5ff3,0x8b,0x01,0x8c,0xa9,0xb8,0x50,0x92,0x33));

#define WRITE_INFO_EVENT(...) TraceLoggingWrite(PROVIDER_HANDLE_VAR, "JavaIpaInfo", TraceLoggingLevel(WINEVENT_LEVEL_INFO), __VA_ARGS__)
#define WRITE_ERROR_EVENT(...) TraceLoggingWrite(PROVIDER_HANDLE_VAR, "JavaIpaError", TraceLoggingLevel(WINEVENT_LEVEL_ERROR), __VA_ARGS__)
#define WRITE_CRITICAL_EVENT(...) TraceLoggingWrite(PROVIDER_HANDLE_VAR, "JavaIpaCritical", TraceLoggingLevel(WINEVENT_LEVEL_CRITICAL), __VA_ARGS__)

JNIEXPORT void JNICALL Java_com_microsoft_applicationinsights_internal_etw_EtwProvider_cppWriteEvent(JNIEnv * env, jobject objJavaThis, 
    jint jiEventId, jstring jstrEventName, jint jiLevel, jstring jstrExtensionVersion, jstring jstrSubscriptionId, jstring jstrAppName, jstring jstrResourceType, jstring jstrLogger, jstring jstrMessage) {
        // TODO is eventId needed?
        
        jboolean copy = JNI_FALSE;
        // convert all jstrings

        // const char* extensionVersion = env->GetStringUTFChars(jstrExtensionVersion, &copy);
        // const char* subscriptionId = env->GetStringUTFChars(jstrSubscriptionId, &copy);
        // const char* appName = env->GetStringUTFChars(jstrAppName, &copy);
        // const char* resourceType = env->GetStringUTFChars(jstrResourceType, &copy);
        // const char* logger = env->GetStringUTFChars(jstrLogger, &copy);

        // TODO macro/inline function the string handling code
        const char* ccMessage = env->GetStringUTFChars(jstrMessage, &copy);
        if (ccMessage == NULL) {
            jthrowable ex = env->ExceptionOccurred();
            if (ex) {
                DBG("GetStringUTFChars(jstrMessage) failed with exception\n");
                // let exception be thrown
                goto jstrCleanUp;
            } else {
                // otherwise throw a new exception
                jclass cls = env->FindClass("java/lang/IllegalStateException");
                if (cls != NULL) {
                    env->ThrowNew(cls, "Could not load message string.");
                }
                env->DeleteLocalRef(cls);
                goto jstrCleanUp;
            }
        }

        int len_message = 1 + (env->GetStringUTFLength(jstrMessage));
        char* message = new char[len_message];
        errno_t cpyerr = strcpy_s(message, len_message, ccMessage);
        if (cpyerr) {
            std::string errmsg = "strcpy_s failed for message: err=" + std::to_string(cpyerr);
            DBG("%s\n", errmsg.c_str());
            jclass cls = env->FindClass("java/lang/IllegalStateException");
            if (cls != NULL) {
                env->ThrowNew(cls, errmsg.c_str());
            } // else there's already an exception pending
            env->DeleteLocalRef(cls);
            delete[] message;
            goto jstrCleanUp;
        }
        DBG("Read message: %s\n", message);
        // write message
        // if (IsWindows10OrGreater()) {
        // switch(jiLevel) {
        //     case WINEVENT_LEVEL_CRITICAL:
        //         WRITE_CRITICAL_EVENT(
        //             provider_EtwHandle,
        //             eventName,
        //             TraceLoggingValue(message, "msg")
        //             TraceLoggingValue(extensionVersion, "ExtVer"),
        //             TraceLoggingValue(subscriptionId, "SubscriptionId"),
        //             TraceLoggingValue(appName, "AppName"),
        //             TraceLoggingValue(resourceType, "ResourceType"),
        //             TraceLoggingValue(logger, "Logger"));
        //         break;
        //     case WINEVENT_LEVEL_ERROR:
        //         WRITE_ERROR_EVENT(
        //             provider_EtwHandle,
        //             eventName,
        //             TraceLoggingValue(message, "msg")
        //             TraceLoggingValue(extensionVersion, "ExtVer"),
        //             TraceLoggingValue(subscriptionId, "SubscriptionId"),
        //             TraceLoggingValue(appName, "AppName"),
        //             TraceLoggingValue(resourceType, "ResourceType"),
        //             TraceLoggingValue(logger, "Logger")
        //             );
        //         break;
        //     case WINEVENT_LEVEL_INFO:
        //         WRITE_INFO_EVENT(
        //             provider_EtwHandle,
        //             eventName,
        //             TraceLoggingValue(message, "msg")
        //             TraceLoggingValue(extensionVersion, "ExtVer"),
        //             TraceLoggingValue(subscriptionId, "SubscriptionId"),
        //             TraceLoggingValue(appName, "AppName"),
        //             TraceLoggingValue(resourceType, "ResourceType"),
        //             TraceLoggingValue(logger, "Logger")
        //             );
        //         break;
        //     default:
        //         // TODO throw exception; set error flag? still need to release strings
        // }

        // TraceLoggingWrite(provider_EtwHandle, eventName, 
        //     // TraceLoggingLevel(WINEVENT_LEVEL_INFO), 
        //     TraceLoggingString(message, "msg")
        //     TraceLoggingString(extensionVersion, "ExtVer"),
        //     TraceLoggingString(subscriptionId, "SubscriptionId"),
        //     TraceLoggingString(appName, "AppName"),
        //     TraceLoggingString(resourceType, "ResourceType"),
        //     TraceLoggingString(logger, "Logger")
        //     );

        TraceLoggingRegister(PROVIDER_HANDLE_VAR);
        WRITE_INFO_EVENT(TraceLoggingValue(message, "msg")
            // ,
            // TraceLoggingValue(extensionVersion, "ExtVer"),
            // TraceLoggingValue(subscriptionId, "SubscriptionId"),
            // TraceLoggingValue(appName, "AppName"),
            // TraceLoggingValue(resourceType, "ResourceType"),
            // TraceLoggingValue(logger, "Logger")
            );
        TraceLoggingUnregister(PROVIDER_HANDLE_VAR);
        

        delete[] message;

jstrCleanUp:
        // release strings
        // env->ReleaseStringUTFChars(jstrExtensionVersion, extensionVersion);
        // env->ReleaseStringUTFChars(jstrSubscriptionId, subscriptionId);
        // env->ReleaseStringUTFChars(jstrAppName, appName);
        // env->ReleaseStringUTFChars(jstrResourceType, resourceType);
        // env->ReleaseStringUTFChars(jstrLogger, logger);
        env->ReleaseStringUTFChars(jstrMessage, ccMessage);
}


// TODO delete me
JNIEXPORT jboolean JNICALL Java_com_microsoft_applicationinsights_internal_etw_EtwProvider_cppIsProviderEnabled(JNIEnv * env, jobject objJavaThis, 
    jint level) {
        // jint<signed 32 bits> -> UCHAR<unsigned char/8 bits>
        BOOLEAN enabled = TraceLoggingProviderEnabled(provider_EtwHandle, level, EVENT_KEYWORD_FILTER_ALL);

        DBG("TraceLoggingProviderEnabled, level=%u: %u\n", level, enabled);

        return enabled;
}