import ballerina.net.http;
import ballerinax.docker;

endpoint<http:Service> backendEP {
    port:9090
}

@http:serviceConfig {
    basePath:"/helloWorld",
    endpoints:[backendEP]
}
@docker:configuration{}
service<http:Service> helloWorld {
    resource sayHello (http:ServerConnector conn, http:Request request) {
        http:Response response = {};
        response.setStringPayload("Hello, World from service helloWorld ! ");
        _ = conn -> respond(response);
    }
}