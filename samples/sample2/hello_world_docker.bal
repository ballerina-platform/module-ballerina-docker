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

@http:ServiceConfig {
      basePath: "/helloWorld"
}
@docker:Config {
    registry: "docker.abc.com",
    name: "helloworld",
    tag: "v1.0"
}
service helloWorld on helloWorldEP {
    resource function sayHello (http:Caller outboundEP, http:Request request) {
        http:Response response = new;
        response.setTextPayload("Hello, World! \n");
        var responseResult = outboundEP->respond(response);
        if (responseResult is error) {
            log:printError("error responding back to client.", responseResult);
        }
    }
}
