import ballerina/http;
//Adding the import as `ballerina/docker as _` will generate the Docker image and Dockerfile for the `helloWorld` service.
import ballerina/docker as _;

listener http:Listener helloWorldEP = new(9090);

@http:ServiceConfig {
    basePath: "/helloWorld"
}
service http:Service /helloWorld on helloWorldEP {
    resource function get sayHello(http:Caller caller) {
        var responseResult = caller->ok("Hello World from Docker! \n");
    }
}
