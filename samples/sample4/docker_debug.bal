import ballerina/http;
import ballerina/log;
import ballerina/docker;

@docker:Expose {}
listener http:Listener helloWorldEP = new(9090);

@docker:Config {
    enableDebug: true,
    name: "helloworld-debug"
}
service http:Service /helloWorld on helloWorldEP {
    resource function get sayHello(http:Caller caller) {
        var responseResult = caller->respond("Hello, World from service helloWorld ! \n");
        if (responseResult is error) {
            log:printError("error responding back to client.", err = responseResult);
        }
    }
}
