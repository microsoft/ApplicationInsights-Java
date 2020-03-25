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
#include "str_value.h"
#include <winmeta.h>
#include <string>
#include <errno.h>

#ifndef NDEBUG
#   include <stdio.h>
#   define DBG(...) printf(__VA_ARGS__)
#elif
#   define DBG(...) do { } while(0)
#endif

#define EVENT_KEYWORD_FILTER_ALL 0x0

#ifndef DLL_FILENAME
#define DLL_FILENAME_STR "applicationinsights-java-etw-provider.dll"
#else
#define DLL_FILENAME_STR STR_VALUE(DLL_FILENAME)
#endif

TRACELOGGING_DEFINE_PROVIDER(
    provider_EtwHandle,
    "Microsoft-ApplicationInsights-Java-IPA",
    // {1f0dc33f-30ae-5ff3-8b01-8ca9b8509233}
    (0x1f0dc33f,0x30ae,0x5ff3,0x8b,0x01,0x8c,0xa9,0xb8,0x50,0x92,0x33));

/**
 * Assigns jstring jstr_##str_var to a new char[len_var] named str_var.
 * str_var must already be declared.
 * If copy fails, and error code is thrown
 *
 */
// TODO enforce max length?
#define EXTRACT_JSTRING(env_var, str_var, len_var, var_id) do \
{ \
    len_var = 1 + (env_var->GetStringUTFLength(jstr_##str_var)); \
    str_var = new char[len_var]; \
    DBG("copying jstr_" #str_var " (len=%d): %p\n", len_var, &jstr_##str_var); \
    str_var = getJavaString(env_var, jstr_##str_var, str_var, len_var); \
    DBG("got " #str_var ": %s\n", str_var); \
} \
while (0)

/********cppInfo(logger, message, extensionVersion, subscriptionId, appName, resourceType)********/
JNIEXPORT void JNICALL Java_com_microsoft_applicationinsights_internal_etw_EtwProvider_cppInfo
    (JNIEnv * env, jobject jobj_javaThis, jstring jstr_logger, jstring jstr_message,
        jstring jstr_extensionVersion, jstring jstr_subscriptionId, jstring jstr_appName, jstring jstr_resourceType)
{
    char * logger = NULL;
    char * message = NULL;
    char * extensionVersion = NULL;
    char * subscriptionId = NULL;
    char * appName = NULL;
    char * resourceType = NULL;
    try
    {
        // convert all jstrings
        int len;
        EXTRACT_JSTRING(env, logger, len, JSTRID_LOGGER);
        EXTRACT_JSTRING(env, message, len, JSTRID_MESSAGE);
        EXTRACT_JSTRING(env, extensionVersion, len, JSTRID_EXTENSION_VERSION);
        EXTRACT_JSTRING(env, subscriptionId, len, JSTRID_SUBSCRIPTION_ID);
        EXTRACT_JSTRING(env, appName, len, JSTRID_APP_NAME);
        EXTRACT_JSTRING(env, resourceType, len, JSTRID_RESOURCE_TYPE);

        // write event
        TraceLoggingRegister(provider_EtwHandle);
        WRITE_INFO_EVENT(
            TraceLoggingValue(message, "msg"),
            TraceLoggingValue(extensionVersion, "ExtVer"),
            TraceLoggingValue(subscriptionId, "SubscriptionId"),
            TraceLoggingValue(appName, "AppName"),
            TraceLoggingValue(resourceType, "ResourceType"),
            TraceLoggingValue(logger, "Logger"));
        TraceLoggingUnregister(provider_EtwHandle);
    }
    catch (jstrerr_t jstrerr)
    {
        handleJstrException(env, jstrerr);
    }
    catch (...)
    {
        handleGenericException(env);
    }

    // clean up
    delete[] message;
    delete[] extensionVersion;
    delete[] subscriptionId;
    delete[] appName;
    delete[] resourceType;
    delete[] logger;
}

/********cppError(logger, message, stackTrace, extensionVersion, subscriptionId, appName, resourceType)********/
JNIEXPORT void JNICALL Java_com_microsoft_applicationinsights_internal_etw_EtwProvider_cppError
    (JNIEnv * env, jobject jobj_javaThis, jstring jstr_logger, jstring jstr_message, jstring jstr_stackTrace,
        jstring jstr_extensionVersion, jstring jstr_subscriptionId, jstring jstr_appName, jstring jstr_resourceType)
{
    DBG("cppError not implemented.\n");
    DBG("FILE: " DLL_FILENAME_STR "\n");
}

/********cppCritical(logger, message, stackTrace, extensionVersion, subscriptionId, appName, resourceType)********/
JNIEXPORT void JNICALL Java_com_microsoft_applicationinsights_internal_etw_EtwProvider_cppCritical
    (JNIEnv * env, jobject jobj_javaThis, jstring jstr_logger, jstring jstr_message, jstring jstr_stackTrace,
        jstring jstr_extensionVersion, jstring jstr_subscriptionId, jstring jstr_appName, jstring jstr_resourceType)
{
    DBG("cppCritical not implemented.\n");
}

inline void handleJstrException(JNIEnv * env, jstrerr_t jstrerr) noexcept {
    jthrowable t = env->ExceptionOccurred();
    if (t) {
        return; // use existing exception
    }
    jclass cls = env->FindClass("java/lang/RuntimeException");
    if (cls != NULL) {
        std::string fieldName;
        switch (jstrerr & 0xFF00) {
            case JSTRID_APP_NAME:
                fieldName = "appName";
                break;
            case JSTRID_EXTENSION_VERSION:
                fieldName = "extensionVersion";
                break;
            case JSTRID_LOGGER:
                fieldName = "logger";
                break;
            case JSTRID_MESSAGE:
                fieldName = "message";
                break;
            case JSTRID_RESOURCE_TYPE:
                fieldName = "resourceType";
                break;
            case JSTRID_STACK_TRACE:
                fieldName = "stackTrace";
                break;
            case JSTRID_SUBSCRIPTION_ID:
                fieldName = "subscriptionId";
                break;
            default:
                fieldName = "unknown";
        }
        std::string errmsg = "cppInfo could not read string from JNI env: " + fieldName;
        env->ThrowNew(cls, errmsg.c_str());
    }
    env->DeleteLocalRef(cls);
}

inline void handleGenericException(JNIEnv * env) noexcept {
    jthrowable t = env->ExceptionOccurred();
    if (t) {
        return; // use existing exception
    }
    jclass cls = env->FindClass("java/lang/RuntimeException");
    if (cls != NULL) {
        env->ThrowNew(cls, "Unknown error from " DLL_FILENAME_STR);
    }
    env->DeleteLocalRef(cls);
}

// TODO update to throw(jstrerr_t)
inline char * getJavaString(JNIEnv * env, jstring &jstr_input, char * cstr_output, int len) throw(jstrerr_t) {
    jboolean copy = JNI_FALSE;
    const char * cc_str = env->GetStringUTFChars(jstr_input, &copy);
    try {
        if (cc_str == NULL) {
            DBG("GetStringUTFChars(jstr_input) failed with exception\n");
            throw JSTRERR_NULL_GETSTR;
        }
        errno_t cpyerr = strcpy_s(cstr_output, len, cc_str);
        if (cpyerr) {
            DBG("strcpy_s failed: errno=%d\n", cpyerr);
            throw JSTRERR_STRCPY;
        }
    }
    catch (jstrerr_t ex)
    {
        DBG("Exception caught: %d\n", ex);
        env->ReleaseStringUTFChars(jstr_input, cc_str);
        throw;
    }

    DBG("jstr/ccstr cleanup: %p\n", &jstr_input);
    env->ReleaseStringUTFChars(jstr_input, cc_str);
    return cstr_output;
}