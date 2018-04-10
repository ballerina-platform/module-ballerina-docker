import ballerina/http;
import ballerinax/docker;

endpoint http:Listener helloWorldEP {
    port:9090
};

@http:ServiceConfig {
      basePath:"/helloWorld"
}
@docker:Config {
    registry:"docker.abc.com",
    name:"helloworld",
    tag:"v1.0"
}
service<http:Service> helloWorld bind helloWorldEP {
    sayHello (endpoint outboundEP, http:Request request) {
        http:Response response = new;
        response.setStringPayload("Hello, World from service helloWorld ! \n");
        _ = outboundEP -> respond(response);
    }
}
