# Ballerina Docker Extension
 
Annotation based docker extension implementation for ballerina. 

[![Build Status](https://wso2.org/jenkins/job/ballerinax/job/docker-pipeline/badge/icon)](https://wso2.org/jenkins/job/ballerinax/job/docker-pipeline/)
![Daily build](https://github.com/ballerina-platform/module-ballerina-docker/workflows/Daily%20build/badge.svg)
![Build master branch](https://github.com/ballerina-platform/module-ballerina-docker/workflows/Build%20master%20branch/badge.svg)
![Trivy Scan](https://github.com/ballerina-platform/module-ballerina-docker/workflows/Trivy%20Docker%20Image%20Scan/badge.svg)
[![codecov](https://codecov.io/gh/ballerina-platform/module-ballerina-docker/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerina-docker)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) 
## Features:
- Dockerfile generation based on @docker:Config annotations. 
- Docker image generation. 
- Docker push support with docker registry.
- Docker based ballerina debug support. 
- Copy file support. 

## Supported Annotations:

### @docker:Config{}
- Supported with ballerina services, listeners or functions.

|**Annotation Name**|**Description**|**Default value**|
|--|--|--|
|name|Name of the Docker image|File name of the generated .jar file|
|registry|Docker registry url|None|
|tag|Docker image tag|latest|
|env|Environment variables for Docker image|None|
|username|Username for Docker registry|None|
|password|Password for Docker registry|None|
|baseImage|Base image to create the Docker image|ballerina/jre8:v1|
|buildImage|Enable building Docker image|true|
|push|Enable pushing Docker image to registry|false|
|enableDebug|Enable debug for ballerina|false|
|debugPort|Remote debug port|5005|
|dockerAPIVersion|Docker API Version|None|
|dockerHost|Docker host IP and docker PORT. ( e.g minikube IP and docker PORT)|DOCKER_HOST environment variable. If DOCKER_HOST is unavailable, uses "unix:///var/run/docker.sock" for Unix or uses "tcp://localhost:2375" for Windows|
|dockerCertPath|Docker certificate path|"DOCKER_CERT_PATH" environment variable|
|cmd|Value for CMD for the generated Dockerfile|`CMD java -jar ${APP} [--b7a.config.file=${CONFIG_FILE}] [--debug]`|
|dockerConfigFile|Docker config file path|None|
|uberJar|Use ballerina uber jar|Default is `false`|

### @docker:CopyFiles{}
- Supported with ballerina services, listeners or functions.

|**Annotation Name**|**Description**|**Default value**|
|--|--|--|
|sourceFile|Source path of the file (in your machine)|None|
|target|Target path (inside container)|None|
|isBallerinaConf|Flag whether file is a ballerina config file|false|

### @docker:Expose{}
- Supported with ballerina listeners.

## How to run

### Prerequisites

1. Download and install JDK 8
1. Get a clone or download the source from [this repository](https://github.com/ballerina-platform/module-ballerina-docker).
1. Export github personal access token & user name as environment variables.
   ```bash
       export packagePAT=<Token>
       export packageUser=<username>
   ```
1. (optional) Specify the Java home path for JDK 8 ie;
    ```bash
        export JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/
    ```
1. (optional) Ensure Docker daemon is running (used for building tests).

### Building

1. Run the corresponding Gradle command from within the `module-ballerina-docker` directory.
```bash
#To build the module:
./gradlew clean build

#To build the module without the tests:
./gradlew clean build :docker-extension-test:prepareDistribution -x test
```
1. Copy `docker-extension/build/docker-extension-***.jar` file to `<BALLERINA_HOME>/bre/lib` directory.
1. Run `ballerina build <.bal filename>` to generate artifacts.

The docker artifacts will be created in a folder called docker with following structure.
```bash
|── docker
|    └── Dockerfile
└── outputfilename.jar
```

### Enable building for Windows Platform
Use the "BAL_DOCKER_WINDOWS=true" environment variable to enable building docker images supporting Windows platform.

### Enabling debug logs
Use the "BAL_DOCKER_DEBUG=true" environment variable to enable docker related debug logs when building the ballerina
source file.

### Annotation Usage Sample:
```ballerina
import ballerina/http;
import ballerina/log;
import ballerina/docker;

@docker:Expose{}
listener http:Listener helloWorldEP = new(9090);

@http:ServiceConfig {
      basePath: "/helloWorld"
}
@docker:Config {
    registry: "docker.abc.com",
    name: "helloworld",
    tag: "v1.0"
}
service helloWorld on helloWorldEP {
    resource function sayHello(http:Caller caller, http:Request request) {
        http:Response response = new;
        response.setTextPayload("Hello, World from service helloWorld ! \n");
        var responseResult = caller->respond(response);
        if (responseResult is error) {
            log:printError("error responding back to client.", err = responseResult);
        }
    }
}
```
**Refer [samples](samples) for more info.**
