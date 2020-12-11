## Module Overview

This module offers an annotation based docker extension implementation for Ballerina. 

- For information on the operations, which you can perform with this module, see [Records](/swan-lake/learn/api-docs/ballerina/docker/index.html#records). 
- For information on the deploymenr, see the [Docker Deployemnt Guide](/swan-lake/learn/deployment/docker/).
- For examples on the usage of the operations, see the [Docker Deployment Example](/swan-lake/learn/by-example/docker-deployment.html).

### Annotation Usage Sample:
```ballerina
import ballerina/http;
import ballerina/log;
import ballerina/docker;

@docker:Expose{}
listener http:Listener helloWorldEP = new(9090);

@docker:Config {
    registry: "docker.abc.com",
    name: "helloworld",
    tag: "v1.0"
}
service http:Service /helloWorld on helloWorldEP {
    resource function get sayHello(http:Caller caller) {
        var responseResult = caller->ok("Hello, World! \n");
        if (responseResult is error) {
            log:printError("error responding back to client.", responseResult);
        }
    }
}
```
