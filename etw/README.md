# Etw Provider for Java Codeless Agent

Provider Name: **`Microsoft-ApplicationInsights-Java-IPA`**
Provider GUID: **`1f0dc33f-30ae-5ff3-8b01-8ca9b8509233`**

# Building

## Build Requirements
To build the native resources, you'll need
* Visual Studio Build Tools, C++
* Windows 10 SDK (developed with `10.0.18362.0`)

Java resources are built with same requirements as the SDK.

The build should find the tools and Windows SDK, but if needed these environment variables are provided to locate the necessary tools and libraries:
* `APPINSIGHTS_WIN10_SDK_PATH`
  * Location of Windows SDK
  * Default: `"%ProgramFiles(x86)%/Windows Kits/10"`
* `APPINSIGHTS_VS_PATH`
  * Location of Visual Studio Build Tools
  * Default: `%ProgramFiles(x86)%/Microsoft Visual Studio 14.0`
* `APPINSIGHTS_WIN_SDK_LIB_PATH`
  * Location of Windows 10 SDK library folder (for linker requirements)
  * Default: `%APPINSIGHTS_WIN10_SDK_PATH%/Lib/10.0.18362.0/um`


## Native Build Tasks
To see full list of tasks, run `gradlew :etw:native:tasks --all`

Native build tasks are of the form `<action><variant><architecture>`. For example, `compileDebugX86` or `linkReleaseX86-64`.

There are only two variants: `Debug` and `Release`, and there are only two architectures: `X86` and `X86-64`.

## Setting Variant for Project Assemble

Use the project property (`-P` option) `ai.etw.native.build` set to the desired native build variant, `debug` or `release`. This property is automatically set to `release` when `-DisRelease=true` is used.

## Enabling verbose logging

The property `ai.etw.native.verbose` accepts a boolean value; when true, it enables verbose logging. This is done with preprocessor directives, so it must be rebuilt to enable. This slows execution time significantly under sufficient load. The build throws an error if this property is true for a relase build.

* compile
  * Compiles the C++ sources.
* link, assemble
  * Links the compiled C++ sources. This also runs the compile task.
  * Produces a DLL
  * Both link and assemble do the same thing.
* preprocess
  * Runs complier with `/P` option. Preprocesses C++ sources to a file. More info in docs: https://docs.microsoft.com/en-us/cpp/build/reference/p-preprocess-to-a-file
  * This can be run without specifying the architecture: `preprocessDebug` and `preprocessRelease` are both valid. This will run the preprocessor for each architecture listed above.

# Unit Tests

There is one test in java which checks that the DLL is extracted properly.

There are two other tests which send 50k and 500k events to ETW. These are disabled if native verbose logging is enabled as it slows the execution time significantly.

These can be disabled manually with the project property `ai.etw.tests.long.disabled` set to `true`.
