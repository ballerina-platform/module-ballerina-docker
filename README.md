# Ballerina Docker Extension
 
Annotation based docker extension implementation for ballerina. 

## Features:
- Dockerfile generation based on @docker:configuration annotations. 
- Docker image generation. 
- Docker push support with docker registry.
- Docker based ballerina debug support. 

## Supported Annotations:

### @docker:configurations{}
|**Annotation Name**|**Description**|**Default value**|
|--|--|--|
|name|Name of the docker image|output balx file name|
|registry|Docker registry|None|
|tag|Docker image tag|latest|
|username|Username for docker registry|None|
|password|Password for docker registry|None|
|push|Push to remote registry|false|
|imageBuild|Whether to build docker image|true|
|baseImage|Base image to create the docker image|ballerina/ballerina:latest|
|debugEnable|Enable debug for ballerina|false|
|debugPort|Remote debug port|5005|


## How to run

1. Download and install JDK 8 or later
2. Get a clone or download the source from this repository (https://github.com/ballerinax/docker)
3. Run the Maven command ``mvn clean  install`` from within the docker directory.
4. Copy ``target/docker-extenstion-0.962.0.jar`` file to ``<BALLERINA_HOME>/bre/lib`` directory.
5. Run ``ballerina build <.bal filename>`` to generate artifacts.

The docker artifacts will be created in a folder called target with following structure.
```bash
target/
├── outputfilename
│   	└── docker
│	      	└── Dockerfile
└── outputfilename.balx
```

### Annotation Usage Sample:
```ballerina
import ballerina.docker;
import ballerina.net.http;

@docker:configuration {
    registry:"docker.abc.com",
    name:"helloworld",
    tag:"v1.0"
}
@http:configuration {
    basePath:"/helloWorld"
}
service<http> helloWorld {
    resource sayHello (http:Connection conn, http:InRequest req) {
        http:OutResponse res = {};
        res.setStringPayload("Hello, World from service helloWorld !");
        _ = conn.respond(res);
    }
}
```
**Refer [samples](samples) for more info.**
