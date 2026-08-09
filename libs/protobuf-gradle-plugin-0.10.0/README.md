# Custom Protobuf Gradle Plugin

This directory contains a customized version of the `protobuf-gradle-plugin`. It is kept here as a standalone codebase for local compilation and maintenance, without directly interfering with the parent project's active Gradle build settings.

## Upstream Base

This version is based on the official Google repository:
- **Upstream Repository**: [google/protobuf-gradle-plugin](https://github.com/google/protobuf-gradle-plugin)
- **Base Commit**: [`8be67f8d98c525273248f5eea9892ee1c7895984`](https://github.com/google/protobuf-gradle-plugin/commit/8be67f8d98c525273248f5eea9892ee1c7895984)

## Modifications & Purpose

### 1. Fix Gradle 9.x+ Duplicate Resource Copying Strategy
- **File modified**: `src/main/groovy/com/google/protobuf/gradle/ProtobufPlugin.groovy`
- **Details**: Inside the `ProtoSyncTask` action, we added `spec.duplicatesStrategy = DuplicatesStrategy.INCLUDE` to the file system sync operation.
- **Purpose**: Starting from Gradle 9.x, Gradle enforces strict checking on duplicate files during copy/sync tasks. If a duplicate `.proto` file exists in the source paths, the build fails with `no duplicate handling strategy has been set`. Setting the strategy to `INCLUDE` restores compatibility with Gradle 9.x+ without altering task behaviors.

### 2. Fix Static Compilation Type Mismatches
- **Files modified**: 
  - `src/main/groovy/com/google/protobuf/gradle/ProtobufPlugin.groovy`
  - `src/main/groovy/com/google/protobuf/gradle/ProtobufAndroidSupport.groovy`
- **Details**: Corrected the return signature of `addGenerateProtoTask` to properly return `TaskProvider<GenerateProtoTask>` instead of a generic `Provider`.
- **Purpose**: Fixes Groovy compiler errors during static type checking when integrating with Android Gradle Plugin (AGP) APIs that strictly require `TaskProvider`.

## Licensing

For license details, please refer to the `LICENSE` file in the root of this plugin directory.