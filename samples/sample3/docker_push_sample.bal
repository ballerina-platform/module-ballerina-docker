import ballerinax.docker;
import ballerina.net.http;

@docker:configuration {
    push:true,
    registry:"index.docker.io/<username>",
    name:"helloworld-push",
    tag:"v2.0.0",
    username:"<username>",
    password:"<password>"
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

