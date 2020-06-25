import ballerina/http;
import ballerina/log;
//Adding the import as `ballerina/docker as _` will generate the Docker image and Dockerfile for the `helloWorld` service.
import ballerina/docker as _;

listener http:Listener helloWorldEP = new(9090);

@http:ServiceConfig {
    basePath: "/helloWorld"
}
service helloWorld on helloWorldEP {
    resource function sayHello(http:Caller outboundEP, http:Request request) {
        http:Response response = new;
        response.setTextPayload("Hello World from Docker ! \n");
        var responseResult = outboundEP->respond(response);
        if (responseResult is error) {
            error err = responseResult;
            log:printError("Error sending response", err);
        }
    }
}
