import ballerina/http;
import ballerinax/docker;

@docker:Expose {}
listener http:Listener helloWorldEP = new(9090);

@docker:Expose {}
listener http:Listener helloWorldEPSecured = new(9696, config = {
    secureSocket: {
        keyStore: {
            path: "${ballerina.home}/bre/security/ballerinaKeystore.p12",
            password: "ballerina"
        }
    }
});

@http:ServiceConfig {
    basePath: "/helloWorld"
}
@docker:Config {}
service helloWorld on helloWorldEP, helloWorldEPSecured {
    resource function sayHello(http:Caller outboundEP, http:Request request) {
        http:Response response = new;
        response.setTextPayload("Hello, World from service helloWorld ! \n");
        _ = outboundEP->respond(response);
    }
}
