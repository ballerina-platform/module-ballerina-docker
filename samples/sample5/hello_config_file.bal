import ballerina/config;
import ballerina/http;
import ballerina/io;
import ballerinax/docker;

@docker:Config {}
@docker:CopyFiles {
    files: [
        { source: "./conf/ballerina.conf", target: "/home/ballerina/conf/ballerina.conf", isBallerinaConf: true },
        { source: "./conf/data.txt", target: "/home/ballerina/data/data.txt" }
    ]
}

@docker:Expose {}
listener http:Server helloWorldEP = new http:Server(9090);

@http:ServiceConfig {
    basePath: "/helloWorld"
}
service helloWorld on helloWorldEP {
    @http:ResourceConfig {
        methods: ["GET"],
        path: "/config/{user}"
    }
    resource function getConfig(http:Caller outboundEP, http:Request request, string user) {
        http:Response response = new;
        string userId = getConfigValue(user, "userid");
        string groups = getConfigValue(user, "groups");
        string payload = "{'userId': '" + userId + "', 'groups': '" + groups + "'}";
        response.setTextPayload(payload + "\n");
        _ = outboundEP->respond(response);
    }

    @http:ResourceConfig {
        methods: ["GET"],
        path: "/data"
    }
    resource function getData(http:Caller outboundEP, http:Request request) {
        http:Response response = new;
        string payload = readFile("./data/data.txt");
        response.setTextPayload("{'data': '" + untaint payload + "'}\n");
        _ = outboundEP->respond(response);
    }
}

function getConfigValue(string instanceId, string property) returns (string) {
    string key = untaint instanceId + "." + untaint property;
    return config:getAsString(key, default = "Invalid User");
}

function readFile(string filePath) returns (string) {
    io:ReadableByteChannel bchannel = io:openReadableFile(filePath);
    io:ReadableCharacterChannel cChannel = new io:ReadableCharacterChannel(bchannel, "UTF-8");

    var readOutput = cChannel.read(50);
    match readOutput {
        string text => {
            return text;
        }
        error ioError => return "Error: Unable to read file";
    }
}
