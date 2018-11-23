import ballerina/http;
import ballerinax/docker;

@docker:Expose {}
listener http:Server helloWorldEP = new http:Server(9090);

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

