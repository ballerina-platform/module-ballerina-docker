import ballerina/http;
import ballerinax/docker;

endpoint http:ServiceEndpoint helloWorldEP {
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
    sayHello (endpoint outboundEP, http:Request request) {
        http:Response response = new;
        response.setStringPayload("Hello, World from service helloWorld ! \n");
        _ = outboundEP -> respond(response);
    }
}

