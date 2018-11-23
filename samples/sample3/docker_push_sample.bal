import ballerina/http;
import ballerinax/docker;

@docker:Expose {}
listener http:Server helloWorldEP = new http:Server(9090);

@docker:Config {
    push:true,
    registry:"index.docker.io/$env{DOCKER_USERNAME}",
    name:"helloworld-push",
    tag:"v2.0.0",
    username:"$env{DOCKER_USERNAME}",
    password:"$env{DOCKER_PASSWORD}"
}
@http:ServiceConfig {
    basePath:"/helloWorld"
}
service helloWorld on helloWorldEP {
    resource function sayHello(http:Caller outboundEP, http:Request request) {
        http:Response response = new;
        response.setTextPayload("Hello, World from service helloWorld ! \n");
        _ = outboundEP->respond(response);
    }
}
