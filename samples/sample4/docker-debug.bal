import ballerina.docker;
import ballerina.net.http;

@docker:configuration {
    debugEnable:true,
    name:"helloworld-debug"
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