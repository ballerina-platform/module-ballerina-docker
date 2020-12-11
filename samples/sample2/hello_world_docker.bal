import ballerina/http;
import ballerina/log;
import ballerina/docker;

@docker:Expose {}
listener http:Listener helloWorldEP = new(9090, {
    secureSocket: {
        keyStore: {
            path: "./ballerinaKeystore.p12",
            password: "ballerina"
        }
    }
});

@docker:Config {
    registry: "docker.abc.com",
    name: "helloworld",
    tag: "v1.0"
}
service http:Service /helloWorld on helloWorldEP {
    resource function get sayHello(http:Caller caller) {
        var responseResult = caller->ok("Hello, World! \n");
        if (responseResult is error) {
            log:printError("error responding back to client.", err = responseResult);
        }
    }
}
