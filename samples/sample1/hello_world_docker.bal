import ballerina/http;
import ballerinax/docker;

@http:ServiceConfig {
    basePath: "/helloWorld"
}
@docker:Config {}
service helloWorld on new http:Listener(9090) {
    resource function sayHello(http:Caller outboundEP, http:Request request) {
        http:Response response = new;
        response.setTextPayload("Hello, World from service helloWorld ! \n");
        _ = outboundEP->respond(response);
    }
}
