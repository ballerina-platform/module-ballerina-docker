import ballerina/http;
import ballerinax/docker;

@http:ServiceConfig {
    basePath:"/helloWorld"
}
@docker:Config {}
service<http:Service> helloWorld bind {} {
    sayHello(endpoint outboundEP, http:Request request) {
        http:Response response = new;
        response.setStringPayload("Hello, World from service helloWorld ! \n");
        _ = outboundEP->respond(response);
    }
}