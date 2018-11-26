import ballerina/http;
import ballerinax/docker;

@docker:Expose {}
listener http:Listener helloWorldEP = new(9090, config = {
    secureSocket: {
        keyStore: {
            path: "${ballerina.home}/bre/security/ballerinaKeystore.p12",
            password: "ballerina"
        }
    }
});

@http:ServiceConfig {
      basePath:"/helloWorld"
}
@docker:Config {
    registry:"docker.abc.com",
    name:"helloworld",
    tag:"v1.0"
}
service helloWorld on helloWorldEP {
    resource function sayHello (http:Caller outboundEP, http:Request request) {
        http:Response response = new;
        response.setTextPayload("Hello, World! \n");
        _ = outboundEP->respond(response);
    }
}
