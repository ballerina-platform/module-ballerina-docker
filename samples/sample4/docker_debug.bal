import ballerina/http;
import ballerinax/docker;

@docker:Expose {}
listener http:Listener helloWorldEP = new(9090);

@docker:Config {
    enableDebug:true,
    name: "helloworld-debug"
}
@http:ServiceConfig {
    basePath: "/helloWorld"
}
service helloWorld on helloWorldEP {
    resource function sayHello(http:Caller outboundEP, http:Request request) {
        http:Response response = new;
        response.setTextPayload("Hello, World from service helloWorld ! \n");
        _ = outboundEP->respond(response);
    }
}
