import ballerina/http;
import ballerina/log;
import ballerina/docker;

@docker:Expose {}
listener http:Listener helloWorldEP = new(9090);

@docker:Expose {}
listener http:Listener helloWorldEPSecured = new(9696, {
    secureSocket: {
        keyStore: {
            path: "./ballerinaKeystore.p12",
            password: "ballerina"
        }
    }
});


@docker:Config {}
service http:Service /helloWorld on new http:Listener(9090) {
    resource function get sayHello(http:Caller caller) {
        var responseResult = caller->ok("Hello, World from service helloWorld ! \n");
        if (responseResult is error) {
            log:printError("error responding back to client.", responseResult);
        }
    }
}