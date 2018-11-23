import ballerina/http;
import ballerinax/docker;

@docker:Expose {}
listener http:Server burgerEP = new http:Server(9096, config = {
    secureSocket: {
        keyStore: {
            path: "${ballerina.home}/bre/security/ballerinaKeystore.p12",
            password: "ballerina"
        }
    }
});

@docker:Config {}
@http:ServiceConfig {
    basePath:"/burger"
}
service BurgerAPI on burgerEP {
    @http:ResourceConfig {
        methods:["GET"],
        path:"/menu"
    }
    resource function getBurgerMenu(http:Caller outboundEP, http:Request request) {
        http:Response response = new;
        response.setTextPayload("Burger menu \n");
        _ = outboundEP->respond(response);
    }
}