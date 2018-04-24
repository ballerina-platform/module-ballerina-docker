import ballerina/http;
import ballerinax/docker;

@docker:Expose {}
endpoint http:Listener helloWorldEP {
    port:9090
};

@docker:Config {
    enableDebug:true,
    name:"helloworld-debug"
}
@http:ServiceConfig {
    basePath:"/helloWorld"
}
service<http:Service> helloWorld bind helloWorldEP {
    sayHello(endpoint outboundEP, http:Request request) {
        http:Response response = new;
        response.setTextPayload("Hello, World from service helloWorld ! \n");
        _ = outboundEP->respond(response);
    }
}

