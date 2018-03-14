import ballerina.net.http;
import ballerinax.docker;

endpoint<http:Service> backendEP {
    port:9090
}
@docker:configuration {
    push:true,
    registry:"index.docker.io/anuruddhal",
    name:"helloworld-push",
    tag:"v2.0.0",
    username:"anuruddhal",
    password:"1qaz2wsx@"
}

@http:serviceConfig {
    basePath:"/helloWorld",
    endpoints:[backendEP]
}
service<http:Service> helloWorld {
    resource sayHello (http:ServerConnector conn, http:Request request) {
        http:Response response = {};
        response.setStringPayload("Hello, World from service helloWorld ! ");
        _ = conn -> respond(response);
    }
}
