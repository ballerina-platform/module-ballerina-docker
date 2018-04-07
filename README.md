# Ballerina Docker Extension
 
Annotation based docker extension implementation for ballerina. 

## Features:
- Dockerfile generation based on @docker:Config annotations. 
- Docker image generation. 
- Docker push support with docker registry.
- Docker based ballerina debug support. 
- Copy file support. 

## Supported Annotations:

### @docker:Config{}
|**Annotation Name**|**Description**|**Default value**|
|--|--|--|
|name|Name of the docker image|output balx file name|
|registry|Docker registry|None|
|tag|Docker image tag|latest|
|buildImage|Whether to build docker image|true|
|dockerHost|Docker host IP and docker PORT. ( e.g minikube IP and docker PORT)|unix:///var/run/docker.sock|
|dockerCertPath|Docker cert path|null|
|baseImage|Base image to create the docker image|ballerina/ballerina:latest|
|enableDebug|Enable debug for ballerina|false|
|debugPort|Remote debug port|5005|
|push|Push to remote registry|false|
|username|Username for docker registry|None|
|password|Password for docker registry|None|

### @docker:CopyFiles{}
|**Annotation Name**|**Description**|**Default value**|
|--|--|--|
|source|source path of the file (in your machine)|None|
|target|target path (inside container)|None|
|isBallerinaConf|flag whether file is a ballerina config file|false|


## How to run

1. Download and install JDK 8 or later
2. Get a clone or download the source from this repository (https://github.com/ballerinax/docker)
3. Run the Maven command ``mvn clean  install`` from within the docker directory.
4. Copy ``target/docker-extenstion-0.9***.jar`` file to ``<BALLERINA_HOME>/bre/lib`` directory.
5. Run ``ballerina build <.bal filename>`` to generate artifacts.

The docker artifacts will be created in a folder called docker with following structure.
```bash
|── docker
|    └── Dockerfile
└── outputfilename.balx
```

### Annotation Usage Sample:
```ballerina
import ballerina/http;
import ballerinax/docker;

endpoint<http:Service> backendEP {
    port:9090
}

@docker:Config {
    registry:"docker.abc.com",
    name:"helloworld",
    tag:"v1.0"
}
@http:ServiceConfig {
    basePath:"/helloWorld",
    endpoints:[backendEP]
}
service<http:Service> helloWorld {
    resource sayHello (http:ServerConnector conn, http:Request request) {
        http:Response response = new;
        response.setStringPayload("Hello, World from service helloWorld ! ");
        _ = conn -> respond(response);
    }
}
```
**Refer [samples](samples) for more info.**