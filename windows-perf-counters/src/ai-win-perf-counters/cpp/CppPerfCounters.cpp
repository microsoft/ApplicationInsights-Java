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

// This is the main DLL file.

#include "CppPerfCounters.h"

#include <string>

using System::Text::Encoding;
using namespace System::Reflection;
using namespace System;
using namespace System::IO;
using namespace System::Collections::Generic;
using namespace System::Diagnostics;

using System::Runtime::InteropServices::Marshal;
using namespace Microsoft;

static const double EXCEPTION_IN_GET_PERF_COuNTER_WRAPPER_FUNCTION_EXCEPTION = -1;
static const double EXCEPTION_WHILE_FETCHING_PERF_COUNTER_VALUE = -7;
static const double EXCEPTION_IN_GET_PERF_COuNTER_INTERNAL_WRAPPER_FUNCTION_EXCEPTION = -4;
static const double PERF_COUNTER_WAS_NOT_FOUND = -2;

ref class PerfCountersUtils
{
private:
	static Dictionary<String^, PerformanceCounter^>^ pcDictionary = gcnew Dictionary<String^, PerformanceCounter^>;

public:
	static String^ getInstanceName(long processId)
	{
		String^ instanceName = GetInstanceNameForProcessId(processId);
		if (instanceName == nullptr)
		{
			return nullptr;
		}

		return instanceName;
	}

	static String^ AddProcessPerformanceCounter(
		String^ performanceCounterCategoryName,
		String^ performanceCounterCounterName,
		String^ performanceCounterInstanceName)
	{
		String^ key = performanceCounterCategoryName + performanceCounterCounterName + performanceCounterInstanceName;
		if (pcDictionary->ContainsKey(key))
		{
			return nullptr;
		}

		PerformanceCounter^ pc = gcnew PerformanceCounter(performanceCounterCategoryName, performanceCounterCounterName, performanceCounterInstanceName);
		pcDictionary->Add(key, pc);
		return key;
	}

	static double GetProcessPerformanceCounterValue(String^ performanceCounterName)
	{
		PerformanceCounter^ pc;
		bool found = pcDictionary->TryGetValue(performanceCounterName, pc);
		if (!found)
		{
			return PERF_COUNTER_WAS_NOT_FOUND;
		}

		try
		{
			double value = pc->NextValue();
			return value;
		}
		catch (...)
		{
			return EXCEPTION_WHILE_FETCHING_PERF_COUNTER_VALUE;
		}
	}

private:
	static String^ GetInstanceNameForProcessId(long processId)
	{
		Process^ process = Process::GetProcessById(processId);
		String^ processName = Path::GetFileNameWithoutExtension(process->ProcessName);

		PerformanceCounterCategory^ cat = gcnew PerformanceCounterCategory("Process");
		array<String^>^ instances = cat->GetInstanceNames();

		for each(String^ instance in instances)
		{
			if (!instance->StartsWith(processName))
			{
				continue;
			}

			PerformanceCounter^ cnt = gcnew PerformanceCounter("Process", "ID Process", instance, true);
			int val = (int)cnt->RawValue;
			delete cnt;
			if (val == processId)
			{
				return instance;
			}
		}

		return nullptr;
	}
};

String^ toString(const char*chars)
{
	int len = (int)strlen(chars);
	array<unsigned char>^ a = gcnew array<unsigned char>(len);
	int i = 0;
	while (i < len)
	{
		a[i] = chars[i];
		++i;
	}

	return Encoding::UTF8->GetString(a);
}

String^ getInstanceName(long processId) {
	return PerfCountersUtils::getInstanceName(processId);
}

String^ addProcessCounter(const char *category, const char *name, const char *instance)
{
	try
	{
		return PerfCountersUtils::AddProcessPerformanceCounter(toString(category), toString(name), toString(instance));
	}
	catch (...)
	{
		return nullptr;
	}
}

double getPerfCounterValue(const char *name) {
	try
	{
		return PerfCountersUtils::GetProcessPerformanceCounterValue(toString(name));
	}
	catch (...)
	{
		return EXCEPTION_IN_GET_PERF_COuNTER_INTERNAL_WRAPPER_FUNCTION_EXCEPTION;
	}
}

void MarshalString(String ^s, std::string& ostr) {
	const char* chars = (const char*)(Marshal::StringToHGlobalAnsi(s)).ToPointer();
	ostr = chars;
	Marshal::FreeHGlobal(IntPtr((void*)chars));
}

JNIEXPORT jstring JNICALL Java_com_microsoft_applicationinsights_internal_perfcounter_JniPCConnector_getInstanceName(
	JNIEnv * env, 
	jclass clz, 
	jint processId)
{
	try
	{
		String^ result = getInstanceName(processId);
		if (result == nullptr)
		{
			return nullptr;
		}
		std::string a;
		MarshalString(result, a);
		jstring r = env->NewStringUTF(a.c_str());
		return r;
	}
	catch (...)
	{
		return false;
	}
}

JNIEXPORT jstring JNICALL Java_com_microsoft_applicationinsights_internal_perfcounter_JniPCConnector_addCounter(
	JNIEnv *env, 
	jclass clz, 
	jstring rawPerformanceCounterCategoryName,
	jstring rawPerformanceCounterCounterName,
	jstring rawPerformanceCounterInstance)
{
	try
	{
		jboolean copy;
		const char *performanceCounterCategoryName = env->GetStringUTFChars(rawPerformanceCounterCategoryName, &copy);
		jboolean copy1;
		const char *performanceCounterCounterName = env->GetStringUTFChars(rawPerformanceCounterCounterName, &copy1);
		jboolean copy2;
		const char *performanceCounterInstance = env->GetStringUTFChars(rawPerformanceCounterInstance, &copy2);
		String^ value = addProcessCounter(performanceCounterCategoryName, performanceCounterCounterName, performanceCounterInstance);
		
		env->ReleaseStringUTFChars(rawPerformanceCounterCategoryName, performanceCounterCategoryName);
		env->ReleaseStringUTFChars(rawPerformanceCounterCounterName, performanceCounterCounterName);
		env->ReleaseStringUTFChars(rawPerformanceCounterInstance, performanceCounterInstance);

		std::string a;
		MarshalString(value, a);
		jstring r = env->NewStringUTF(a.c_str());
		return r;
	}
	catch (...)
	{
		return nullptr;
	}
}

JNIEXPORT jdouble JNICALL Java_com_microsoft_applicationinsights_internal_perfcounter_JniPCConnector_getPerformanceCounterValue(
	JNIEnv *env, 
	jclass clz, 
	jstring name)
{
	try
	{
		jboolean copy;
		const char *performanceCounterName = env->GetStringUTFChars(name, &copy);
		double value = getPerfCounterValue(performanceCounterName);
		env->ReleaseStringUTFChars(name, performanceCounterName);

		return value;
	}
	catch (...)
	{
		return EXCEPTION_IN_GET_PERF_COuNTER_WRAPPER_FUNCTION_EXCEPTION;
	}
}
