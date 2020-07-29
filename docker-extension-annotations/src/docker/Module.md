## Module Overview

This module offers an annotation based docker extension implementation for Ballerina. 

For information on the operations, which you can perform with this module, see [Records](https://ballerina.io/swan-lake/learn/api-docs/ballerina/docker/index.html#records). For examples on the usage of the operations, see the [Docker Deployment Example](https://ballerina.io/swan-lake/learn/by-example/docker-deployment.html).

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
