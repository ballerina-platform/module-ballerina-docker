# Ballerina Docker Extension
 
Annotation based docker extension implementation for ballerina. 

![Daily build](https://github.com/ballerina-platform/module-ballerina-docker/workflows/Daily%20build/badge.svg)
![Build](https://github.com/ballerina-platform/module-ballerina-docker/actions/workflows/build-timestamped-master.yml/badge.svg)
![Trivy Scan](https://github.com/ballerina-platform/module-ballerina-docker/workflows/Trivy%20Docker%20Image%20Scan/badge.svg)
[![codecov](https://codecov.io/gh/ballerina-platform/module-ballerina-docker/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerina-docker)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) 

## Features:
- Dockerfile generation. 
- Docker image generation. 
- Docker based ballerina debug support. 
- Copy file support. 

## How to run

### Prerequisites

1. Download and install JDK 11
1. Get a clone or download the source from [this repository](https://github.com/ballerina-platform/module-ballerina-docker).
1. Export github personal access token & user name as environment variables.
   ```bash
       export packagePAT=<Token>
       export packageUser=<username>
   ```
1. (optional) Specify the Java home path for JDK 11 ie;
    ```bash
        export JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home/
    ```
1. (optional) Ensure Docker daemon is running (used for building tests).

### Building

1. Run the corresponding Gradle command from within the `module-ballerina-docker` directory.
```bash
#To build the module:
./gradlew clean build

#To build the module without the tests:
./gradlew clean build -x test
```
1. Copy `docker-extension/build/docker-generator-***.jar` file to `<BALLERINA_HOME>/bre/lib` directory.

### Enable building for Windows Platform
Use the "BAL_DOCKER_WINDOWS=true" environment variable to enable building docker images supporting Windows platform.

### Enabling debug logs
Use the "BAL_DOCKER_DEBUG=true" environment variable to enable docker related debug logs when building the ballerina
source file.
