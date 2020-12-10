import ballerina/http;
import ballerina/log;
import ballerina/docker;

@docker:Expose {}
listener http:Listener pizzaEP = new(9099);

@docker:Config {}
service http:Service /pizza on new http:Listener(9090) {
    resource function get menu(http:Caller caller) {
        var responseResult = caller->ok("Pizza menu \n");
        if (responseResult is error) {
            log:printError("error responding back to client.", responseResult);
        }
    }
}
