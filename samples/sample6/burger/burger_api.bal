import ballerina/http;
import ballerinax/docker;

@docker:Expose {}
endpoint http:Listener burgerEP {
    port: 9096,
    secureSocket: {
        keyStore: {
            path: "${ballerina.home}/bre/security/ballerinaKeystore.p12",
            password: "ballerina"
        }
    }
};


@docker:Config {}
@http:ServiceConfig {
    basePath: "/burger"
}
service<http:Service> BurgerAPI bind burgerEP {
    @http:ResourceConfig {
        methods: ["GET"],
        path: "/menu"
    }
    getBurgerMenu(endpoint outboundEP, http:Request req) {
        http:Response response = new;
        response.setTextPayload("Burger menu \n");
        _ = outboundEP->respond(response);
    }
}