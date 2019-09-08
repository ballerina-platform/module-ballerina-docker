# Ballerina Docker Extension
 
Annotation based docker extension implementation for ballerina. 

[![Build Status](https://wso2.org/jenkins/job/ballerinax/job/docker/badge/icon)](https://wso2.org/jenkins/job/ballerinax/job/docker/)
[![Build Status](https://img.shields.io/travis/ballerinax/docker.svg?logo=travis)](https://travis-ci.org/ballerinax/docker)
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
- Supported with ballerina services or listeners.

|**Annotation Name**|**Description**|**Default value**|
|--|--|--|
|name|Name of the docker image|File name of the generated balx file|
|registry|Docker registry url|None|
|tag|Docker image tag|latest|
|username|Username for docker registry|None|
|password|Password for docker registry|None|
|baseImage|Base image to create the docker image|ballerina/ballerina-runtime:<BALLERINA_VERSION>|
|buildImage|Enable building docker image|true|
|push|Enable pushing docker image to registry|false|
|enableDebug|Enable debug for ballerina|false|
|debugPort|Remote debug port|5005|
|dockerAPIVersion|Docker API Version|None|
|dockerHost|Docker host IP and docker PORT. ( e.g minikube IP and docker PORT)|DOCKER_HOST environment variable. If DOCKER_HOST is unavailable, uses "unix:///var/run/docker.sock" for Unix or uses "npipe:////./pipe/docker_engine" for Windows 10 or uses "localhost:2375"|
|dockerCertPath|Docker certificate path|"DOCKER_CERT_PATH" environment variable|

### @docker:CopyFiles{}
- Supported with ballerina services or listeners.

|**Annotation Name**|**Description**|**Default value**|
|--|--|--|
|source|Source path of the file (in your machine)|None|
|target|Target path (inside container)|None|
|isBallerinaConf|Flag whether file is a ballerina config file|false|

### @docker:Expose{}
- Supported with ballerina listeners.

## How to run

1. Download and install JDK 8 or later
2. Get a clone or download the source from this repository (https://github.com/ballerinax/docker)
3. Run the Maven command ``mvn clean  install`` from within the docker directory.
4. Copy ``target/docker-extension-0.9***.jar`` file to ``<BALLERINA_HOME>/bre/lib`` directory.
5. Run ``ballerina build <.bal filename>`` to generate artifacts.

The docker artifacts will be created in a folder called docker with following structure.
```bash
|── docker
|    └── Dockerfile
└── outputfilename.balx
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
