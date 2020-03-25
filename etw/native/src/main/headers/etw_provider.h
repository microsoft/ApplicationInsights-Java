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

#pragma once

#include <windows.h>
#include <TraceLoggingProvider.h>
#include "jni_etw_provider.h"

TRACELOGGING_DECLARE_PROVIDER(provider_EtwHandle);

#define WRITE_INFO_EVENT(...) TraceLoggingWrite(provider_EtwHandle, "JavaIpaInfo", TraceLoggingLevel(WINEVENT_LEVEL_INFO), __VA_ARGS__)
#define WRITE_ERROR_EVENT(...) TraceLoggingWrite(provider_EtwHandle, "JavaIpaError", TraceLoggingLevel(WINEVENT_LEVEL_ERROR), __VA_ARGS__)
#define WRITE_CRITICAL_EVENT(...) TraceLoggingWrite(provider_EtwHandle, "JavaIpaCritical", TraceLoggingLevel(WINEVENT_LEVEL_CRITICAL), __VA_ARGS__)

typedef int jstrerr_t;

#define JSTRERR_SUCCESS     0x00
#define JSTRERR_NULL_GETSTR 0x01
#define JSTRERR_STRCPY      0x02

#define JSTRID_MESSAGE            0x0100
#define JSTRID_LOGGER             0x0200
#define JSTRID_STACK_TRACE        0x0300
#define JSTRID_EXTENSION_VERSION  0x0400
#define JSTRID_SUBSCRIPTION_ID    0x0500
#define JSTRID_APP_NAME           0x0600
#define JSTRID_RESOURCE_TYPE      0x0700

#define STR_MAX_BUFF_SIZE   2048

/**
 * preconditions: char * is allocated, JNIenv and jstring not null, int > 0
 * postconditions:
 * returns: 0 on success,
 */
inline char * getJavaString(JNIEnv *, jstring&, char *, int);

inline void handleJstrException(JNIEnv *, jstrerr_t) noexcept;

inline void handleGenericException(JNIEnv *) noexcept;