import ballerina/http;
import ballerina/log;
import ballerina/docker;

@docker:Config {
}
service http:Service /helloWorld on new http:Listener(9090) {
    resource function get sayHello(http:Caller caller) {
        var responseResult = caller->ok("Hello, World from service helloWorld ! \n");
        if (responseResult is error) {
            log:printError("error responding back to client.", err = responseResult);
        }
    }
}
