import ballerina/http;
import ballerina/log;
import ballerinax/docker;

@docker:Expose {}
listener http:Listener burgerEP = new(9096, config = {
    secureSocket: {
        keyStore: {
            path: "${ballerina.home}/bre/security/ballerinaKeystore.p12",
            password: "ballerina"
        }
    }
});

@docker:Config {}
@http:ServiceConfig {
    basePath: "/burger"
}
service BurgerAPI on burgerEP {
    @http:ResourceConfig {
        methods: ["GET"],
        path: "/menu"
    }
    resource function getBurgerMenu(http:Caller outboundEP, http:Request request) {
        http:Response response = new;
        response.setTextPayload("Burger menu \n");
        var responseResult = outboundEP->respond(response);
        if (responseResult is error) {
            log:printError("error responding back to client.", err = responseResult);
        }
    }
}
