import ballerina/http;
import ballerina/log;
import ballerina/docker;

@docker:Expose {}
listener http:Listener pizzaEP = new(9099);

@docker:Config {}
@http:ServiceConfig {
    basePath: "/pizza"
}
service PizzaAPI on pizzaEP {
    @http:ResourceConfig {
        methods: ["GET"],
        path: "/menu"
    }
    resource function getPizzaMenu(http:Caller outboundEP, http:Request request) {
        http:Response response = new;
        response.setTextPayload("Pizza menu \n");
        var responseResult = outboundEP->respond(response);
        if (responseResult is error) {
            log:printError("error responding back to client.", responseResult);
        }
    }
}
