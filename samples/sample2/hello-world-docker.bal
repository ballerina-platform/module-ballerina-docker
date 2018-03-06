import ballerina.docker;
import ballerina.net.http;

@docker:configuration {
    registry:"docker.abc.com",
    name:"helloworld",
    tag:"v1.0"
}
@http:configuration {
    basePath:"/helloWorld"
}
service<http> helloWorld {
    resource sayHello (http:Connection conn, http:InRequest req) {
        http:OutResponse res = {};
        res.setStringPayload("Hello, World from service helloWorld !");
        _ = conn.respond(res);
    }
}

