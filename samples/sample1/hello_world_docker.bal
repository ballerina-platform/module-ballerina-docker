import ballerina/http;
import ballerinax/docker;

@http:ServiceConfig {
    basePath:"/helloWorld"
}
@docker:Config {}
service<http:Service> helloWorld bind {port:9090} {
    sayHello(endpoint outboundEP, http:Request request) {
        http:Response response = new;
        response.setTextPayload("Hello, World from service helloWorld ! \n");
        _ = outboundEP->respond(response);
    }
}