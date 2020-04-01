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
#include <cstdlib>
#include <cerrno>

#if !defined(NDEBUG) && defined(AIETW_VERBOSE)
#include <cstdio>
#define DBG(...) printf(__VA_ARGS__)
#else
#define DBG(...) do { } while(0)
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

/********cppWriteEvent(IpaEtwEventBase event)********/
JNIEXPORT void JNICALL Java_com_microsoft_applicationinsights_internal_etw_EtwProvider_cppWriteEvent
    (JNIEnv * env, jobject jobj_javaThis, jobject jobj_event)
{
    try
    {
        DBG("getting event_id");
        int event_id = getEventId(env, jobj_event);
        DBG("event_id=%d\n", event_id);
        switch (event_id) {
            case EVENTID_INFO:
            case EVENTID_ERROR:
            case EVENTID_CRITICAL:
            case EVENTID_WARN:
                writeEvent_IpaEtwEvent(env, jobj_event, event_id);
                break;
            default:
                throw (AIJNIERR_UNKONWN_EVENTID | (event_id << 8));
        }
    }
    catch (aijnierr_t err)
    {
        handleJstrException(env, err);
    }
    catch (...)
    {
        handleGenericException(env);
    }

}

int getEventId(JNIEnv * env, jobject &jobj_event) throw(aijnierr_t) {
    jclass cls = env->GetObjectClass(jobj_event);
    jmethodID jmid = env->GetMethodID(cls, "id", "()Lcom/microsoft/applicationinsights/internal/etw/events/model/IpaEtwEventId;");
    if (jmid == NULL) {
        throw AIJNIERR_METHOD_NAME;
    }
    jobject jobj_eventIdEnum = env->CallObjectMethod(jobj_event, jmid);
    if (env->ExceptionCheck()) {
        throw AIJNIERR_EXCEPTION_RAISED;
    }
    cls = env->GetObjectClass(jobj_eventIdEnum);
    jmid = env->GetMethodID(cls, "value", "()I");
    if (jmid == NULL) {
        throw AIJNIERR_METHOD_NAME;
    }
    int value = (int)env->CallObjectMethod(jobj_eventIdEnum, jmid);
    if(env->ExceptionCheck()) {
        throw AIJNIERR_EXCEPTION_RAISED;
    } else {
        return value;
    }
}

#define ETW_FIELD_LOGGER            "Logger"
#define ETW_FIELD_MESSAGE           "msg"
#define ETW_FIELD_EXTENSION_VERSION "ExtVer"
#define ETW_FIELD_SUBSCRIPTION_ID   "SubscriptionId"
#define ETW_FIELD_APPNAME           "AppName"
#define ETW_FIELD_RESOURCE_TYPE     "ResourceType"
#define ETW_FIELD_IKEY              "InstrumentationKey"
#define ETW_FIELD_STACKTRACE        "Exception"
#define ETW_FIELD_OPERATION         "Operation"


void writeEvent_IpaEtwEvent(JNIEnv * env, jobject &jobj_event, int event_id) noexcept {
    char * logger = NULL;
    char * message = NULL;
    char * extensionVersion = NULL;
    char * subscriptionId = NULL;
    char * appName = NULL;
    char * resourceType = NULL;
    char * ikey = NULL;
    char * stackTrace = NULL;
    char * operation = NULL;
    try
    {
        // convert all jstrings
        logger = stringGetter2cstr(env, jobj_event, "getLogger", logger, JSTRID_LOGGER);
        extensionVersion = stringGetter2cstr(env, jobj_event, "getExtensionVersion", extensionVersion, JSTRID_EXTENSION_VERSION);
        message = stringGetter2cstr(env, jobj_event, "getFormattedMessage", message, JSTRID_MESSAGE);
        subscriptionId = stringGetter2cstr(env, jobj_event, "getSubscriptionId", subscriptionId, JSTRID_SUBSCRIPTION_ID);
        appName = stringGetter2cstr(env, jobj_event, "getAppName", appName, JSTRID_APP_NAME);
        resourceType = stringGetter2cstr(env, jobj_event, "getResourceType", resourceType, JSTRID_RESOURCE_TYPE);
        ikey = stringGetter2cstr(env, jobj_event, "getInstrumentationKey", ikey, JSTRID_IKEY);
        operation = stringGetter2cstr(env, jobj_event, "getOperation", operation, JSTRID_OPERATION);

        // write event
        TraceLoggingRegister(provider_EtwHandle);
        if (event_id == EVENTID_INFO) {
            WRITE_INFO_EVENT(
                TraceLoggingValue(message, ETW_FIELD_MESSAGE),
                TraceLoggingValue(extensionVersion, ETW_FIELD_EXTENSION_VERSION),
                TraceLoggingValue(subscriptionId, ETW_FIELD_SUBSCRIPTION_ID),
                TraceLoggingValue(appName, ETW_FIELD_APPNAME),
                TraceLoggingValue(resourceType, ETW_FIELD_RESOURCE_TYPE),
                TraceLoggingValue(logger, ETW_FIELD_LOGGER),
                TraceLoggingValue(ikey, ETW_FIELD_IKEY),
                TraceLoggingValue(operation, ETW_FIELD_OPERATION));
            DBG("\nwrote INFO");
        } else { // all other event types could have Stacktrace data
            stackTrace = stringGetter2cstr(env, jobj_event, "getStacktraceString", stackTrace, JSTRID_STACK_TRACE);
            switch(event_id) {
                case EVENTID_WARN:
                    WRITE_WARN_EVENT(
                        TraceLoggingValue(message, ETW_FIELD_MESSAGE),
                        TraceLoggingValue(extensionVersion, ETW_FIELD_EXTENSION_VERSION),
                        TraceLoggingValue(subscriptionId, ETW_FIELD_SUBSCRIPTION_ID),
                        TraceLoggingValue(appName, ETW_FIELD_APPNAME),
                        TraceLoggingValue(resourceType, ETW_FIELD_RESOURCE_TYPE),
                        TraceLoggingValue(logger, ETW_FIELD_LOGGER),
                        TraceLoggingValue(ikey, ETW_FIELD_IKEY),
                        TraceLoggingValue(operation, ETW_FIELD_OPERATION),
                        TraceLoggingValue(stackTrace, ETW_FIELD_STACKTRACE));
                    DBG("\nwrote WARN");
                    break;
                case EVENTID_ERROR:
                    WRITE_ERROR_EVENT(
                        TraceLoggingValue(message, ETW_FIELD_MESSAGE),
                        TraceLoggingValue(extensionVersion, ETW_FIELD_EXTENSION_VERSION),
                        TraceLoggingValue(subscriptionId, ETW_FIELD_SUBSCRIPTION_ID),
                        TraceLoggingValue(appName, ETW_FIELD_APPNAME),
                        TraceLoggingValue(resourceType, ETW_FIELD_RESOURCE_TYPE),
                        TraceLoggingValue(logger, ETW_FIELD_LOGGER),
                        TraceLoggingValue(ikey, ETW_FIELD_IKEY),
                        TraceLoggingValue(operation, ETW_FIELD_OPERATION),
                        TraceLoggingValue(stackTrace, ETW_FIELD_STACKTRACE));
                    DBG("\nwrote ERROR");
                    break;
                case EVENTID_CRITICAL:
                    WRITE_CRITICAL_EVENT(
                        TraceLoggingValue(message, ETW_FIELD_MESSAGE),
                        TraceLoggingValue(extensionVersion, ETW_FIELD_EXTENSION_VERSION),
                        TraceLoggingValue(subscriptionId, ETW_FIELD_SUBSCRIPTION_ID),
                        TraceLoggingValue(appName, ETW_FIELD_APPNAME),
                        TraceLoggingValue(resourceType, ETW_FIELD_RESOURCE_TYPE),
                        TraceLoggingValue(logger, ETW_FIELD_LOGGER),
                        TraceLoggingValue(ikey, ETW_FIELD_IKEY),
                        TraceLoggingValue(operation, ETW_FIELD_OPERATION),
                        TraceLoggingValue(stackTrace, ETW_FIELD_STACKTRACE));
                    DBG("\nwrote CRITICAL");
                    break;
            }
        }
        TraceLoggingUnregister(provider_EtwHandle);
        DBG(" event:\n\tmsg=%s,\n\tExtVer=%s,\n\tSubscriptionId=%s,\n\tAppName=%s,\n\tResourceType=%s,\n\tLogger=%s\n\tIkey=%s\n\tOperation=%s\n", message, extensionVersion, subscriptionId, appName, resourceType, logger, ikey, operation);
#if !defined(NDEBUG) && defined(AIETW_VERBOSE)
        if (stackTrace != NULL) {
            DBG("\tException=%s\n\n", stackTrace);
        } else {
            DBG("\n");
        }
#endif
    }
    catch (aijnierr_t jnierr)
    {
        handleJstrException(env, jnierr);
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
    delete[] ikey;
    delete[] stackTrace;
    delete[] operation;
}

char * stringGetter2cstr(JNIEnv * env, jobject &jobj_target, const char * method_name, char * rval, aijnierr_t field_id) throw(aijnierr_t) {
    DBG("%s %s\n", __func__, method_name);
    jclass cls_target = env->GetObjectClass(jobj_target);
    jmethodID jmid = env->GetMethodID(cls_target, method_name, "()Ljava/lang/String;");
    if (jmid == NULL) {
        DBG("No method named '%s'\n", method_name);
        throw (AIJNIERR_METHOD_NAME | field_id);
    }
    DBG("Calling %s %p\n", method_name, &jmid);
    jstring jstr_value = (jstring)env->CallObjectMethod(jobj_target, jmid);
    if (env->ExceptionCheck()) {
        throw (AIJNIERR_EXCEPTION_RAISED | field_id);
    }
    if (!jstr_value) {
        DBG("null jstr_value for %s\n", jstrid2name(field_id).c_str());
        throw (AIJNIERR_NULL_GETSTR | field_id);
    }
    return jstring2cstr(env, jstr_value, rval, field_id);
}

void handleJstrException(JNIEnv * env, aijnierr_t jnierr) noexcept {
    std::string message;
    switch(jnierr & 0xFF) {
        case AIJNIERR_METHOD_NAME:
            if (jnierr & 0xFF00) {
                message = "Could not find expected method for " + jstrid2name(jnierr);
            } else {
                message = "Could not find expected method";
            }
            break;
        case AIJNIERR_NULL_GETSTR:
            if (jnierr & 0xFF00) {
                message = "Could not load string value for " + jstrid2name(jnierr);
            } else {
                message = "Could not load string value";
            }
            break;
        case AIJNIERR_STRCPY:
            if (jnierr & 0xFF00) {
                message = "Could not copy string value for " + jstrid2name(jnierr);
            } else {
                message = "Could not copy string value";
            }
            break;
        case AIJNIERR_EXCEPTION_RAISED:
            message = "Exception raised";
            break;
        case AIJNIERR_UNKONWN_EVENTID:
            message = "Unknown event ID: " + std::to_string(jnierr >> 2);
        default:
            message = "Unknown error";
    }
    javaThrowJniException(env, message);
}

std::string jstrid2name(int jnierr) noexcept {
    switch (jnierr & 0xFF00) {
        case JSTRID_APP_NAME:
            return ETW_FIELD_APPNAME;
        case JSTRID_EXTENSION_VERSION:
            return ETW_FIELD_EXTENSION_VERSION;
        case JSTRID_LOGGER:
            return ETW_FIELD_LOGGER;
        case JSTRID_MESSAGE:
            return ETW_FIELD_MESSAGE;
        case JSTRID_RESOURCE_TYPE:
            return ETW_FIELD_RESOURCE_TYPE;
        case JSTRID_STACK_TRACE:
            return ETW_FIELD_STACKTRACE;
        case JSTRID_SUBSCRIPTION_ID:
            return ETW_FIELD_SUBSCRIPTION_ID;
        case JSTRID_IKEY:
            return ETW_FIELD_IKEY;
        case JSTRID_OPERATION:
            return ETW_FIELD_OPERATION;
        default:
            return "unknown";
    }
}

void handleGenericException(JNIEnv * env) noexcept {
    jthrowable t = env->ExceptionOccurred();
    if (t) {
        return; // use existing exception
    }
    javaThrowUnknownError(env, "");
}

jthrowable newJniException(JNIEnv * env, const char * message) noexcept {
    jclass excls = env->FindClass("com/microsoft/applicationinsights/internal/etw/ApplicationInsightsEtwException");
    jmethodID init_id = env->GetMethodID(excls, "<init>", "(Ljava/lang/String;)V");
    jthrowable rval = NULL;
    if (init_id == NULL) {
        DBG("Could not find constructor ApplicationInsightsEtwException(String)");
        javaThrowUnknownError(env, " - could not find ApplicationInsightsEtwException.<init>(String)");
    } else {
        jstring jstr_message = env->NewStringUTF(message);
        rval = (jthrowable)env->NewObject(excls, init_id, jstr_message);
        if (env->ExceptionCheck()) {
            DBG("Exception from ApplicationInsightsEtwException.<init>(String)");
            rval = NULL;
        }
        env->DeleteLocalRef(jstr_message);
    }
    env->DeleteLocalRef(excls);
    return rval;
}

jthrowable newJniException(JNIEnv * env, const char * message, jthrowable cause) noexcept {
    jclass excls = env->FindClass("com/microsoft/applicationinsights/internal/etw/ApplicationInsightsEtwException");
    jmethodID init_id = env->GetMethodID(excls, "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V");
    jthrowable rval = NULL;
    if (init_id == NULL) {
        DBG("Could not find constructor ApplicationInsightsEtwException(String, Throwable)");
        javaThrowUnknownError(env, " - could not find ApplicationInsightsEtwException.<init>(String, Throwable)");
    } else {
        jstring jstr_message = env->NewStringUTF(message);
        rval = (jthrowable)env->NewObject(excls, init_id, jstr_message, cause);
        if (env->ExceptionCheck()) {
            DBG("Exception from ApplicationInsightsEtwException.<init>(String, Throwable)");
            rval = NULL;
        }
        env->DeleteLocalRef(jstr_message);
    }
    env->DeleteLocalRef(excls);
    return rval;
}

void javaThrowJniException(JNIEnv * env, std::string message) noexcept {
    jthrowable cause = env->ExceptionOccurred(); // save the cause
    jthrowable t;
    env->ExceptionClear(); // for future exception check
    if (cause) {
        t = newJniException(env, message.c_str(), cause);
    } else {
        t = newJniException(env, message.c_str());
    }

    if (t) {
        env->Throw(t);
    } else {
        if (!cause) {
            javaThrowUnknownError(env, ": Error creating exception");
        } else {
            env->Throw(cause);
        }

    }
    env->DeleteLocalRef(t);
}

void javaThrowUnknownError(JNIEnv * env, std::string message) noexcept {
    jclass cls = env->FindClass("java/lang/RuntimeException");
    if (cls != NULL) {
        std::string m = "Unknown error from " DLL_FILENAME_STR + message;
        env->ThrowNew(cls, m.c_str());
    }
    env->DeleteLocalRef(cls);
}

char * jstring2cstr(JNIEnv * env, jstring &jstr_input, char * cstr_output, aijnierr_t field_id) throw(aijnierr_t) {
    const char * cc_str = NULL;
    try {
        jboolean copy = JNI_FALSE;
        int len = 1 + (env->GetStringUTFLength(jstr_input));
        DBG("LEN=%d\n", len);
        cc_str = env->GetStringUTFChars(jstr_input, &copy);
        DBG("get jstr chars (%s): js=%p, cc=%p\n", jstrid2name(field_id).c_str(), &cc_str, &jstr_input);
        if (cc_str == NULL) {
            DBG("GetStringUTFChars(jstr_input) failed with exception\n");
            throw (AIJNIERR_NULL_GETSTR | field_id);
        }

        if (len > STR_MAX_BUFF_SIZE) {
            len = STR_MAX_BUFF_SIZE;
        }
        cstr_output = new char[len];
        errno_t cpyerr = strncpy_s(cstr_output, len, cc_str, _TRUNCATE);
        if (cpyerr != 0 && cpyerr != STRUNCATE) {
            DBG("strncpy_s failed: len=%d, errno=%d\n", len, cpyerr);
            throw (AIJNIERR_STRCPY | field_id);
        }
#if !defined(NDEBUG) && defined(AIETW_VERBOSE)
        if (cpyerr == STRUNCATE) {
            DBG("TRUNCATE!\n");
        }
#endif
    }
    catch (aijnierr_t ex)
    {
        DBG("Exception caught: %d\n", ex);
        DBG("rls jstr chars (%s): js=%p, cc=%p\n", jstrid2name(field_id).c_str(), &cc_str, &jstr_input);
        env->ReleaseStringUTFChars(jstr_input, cc_str);
        throw;
    }
    DBG("rls jstr chars (%s): js=%p, cc=%p\n", jstrid2name(field_id).c_str(), &cc_str, &jstr_input);
    env->ReleaseStringUTFChars(jstr_input, cc_str);
    return cstr_output;
}