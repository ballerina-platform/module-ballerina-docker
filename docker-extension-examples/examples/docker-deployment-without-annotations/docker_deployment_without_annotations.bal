import ballerina/http;
//Adding the import as `ballerina/docker as _` will generate the Docker image and Dockerfile for the `helloWorld` service.
import ballerina/docker as _;

listener http:Listener helloWorldEP = new(9090);

@http:ServiceConfig {
    basePath: "/helloWorld"
}
service helloWorld on helloWorldEP {
    resource function sayHello(http:Caller outboundEP, http:Request request) returns error? {
        check outboundEP->respond("Hello World from Docker! \n");
    }
}
