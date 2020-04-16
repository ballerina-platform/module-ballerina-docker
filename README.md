# Ballerina Docker Extension
 
Annotation based docker extension implementation for ballerina. 

[![Build Status](https://wso2.org/jenkins/job/ballerinax/job/docker-pipeline/badge/icon)](https://wso2.org/jenkins/job/ballerinax/job/docker-pipeline/)
[![Build Status](https://img.shields.io/travis/ballerinax/docker.svg?logo=travis)](https://travis-ci.org/ballerinax/docker)
[![Trivy Scan](https://github.com/ballerinax/docker/workflows/Trivy%20Docker%20Image%20Scan%20Workflow/badge.svg)](https://github.com/ballerinax/docker/actions?query=workflow%3A%22Trivy+Docker+Image+Scan+Workflow%22)
[![codecov](https://codecov.io/gh/ballerinax/docker/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerinax/docker)
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

1. Download and install JDK 8 or later
2. Get a clone or download the source from this repository (https://github.com/ballerinax/docker)
3. Run the Gradle command ``gradle build`` from within the docker directory.
4. Copy ``build/docker-extension-0.9***.jar`` file to ``<BALLERINA_HOME>/bre/lib`` directory.
5. Run ``ballerina build <.bal filename>`` to generate artifacts.

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
