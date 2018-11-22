import ballerina/http;
import ballerinax/docker;

@docker:Expose {}
endpoint http:Listener pizzaEP {
    port: 9099
};

@docker:Config {}
@http:ServiceConfig {
    basePath: "/pizza"
}
service<http:Service> PizzaAPI bind pizzaEP {
    @http:ResourceConfig {
        methods: ["GET"],
        path: "/menu"
    }
    getPizzaMenu(endpoint outboundEP, http:Request req) {
        http:Response response = new;
        response.setTextPayload("Pizza menu \n");
        _ = outboundEP->respond(response);
    }
}