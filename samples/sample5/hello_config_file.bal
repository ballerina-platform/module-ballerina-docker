import ballerina/config;
import ballerina/http;
import ballerina/log;
import ballerina/io;
import ballerina/docker;

@docker:Config {}
@docker:CopyFiles {
    files: [
        { sourceFile: "./conf/ballerina.conf", target: "/home/ballerina/conf/ballerina.conf", isBallerinaConf: true },
        { sourceFile: "./conf/data.txt", target: "/home/ballerina/data/data.txt" }
    ]
}

@docker:Expose {}
listener http:Listener helloWorldEP = new(9090);

service http:Service /helloWorld on new http:Listener(9090) {
    resource function get config/[string user](http:Caller caller, http:Request request) returns @tainted error? {
        http:Response response = new;
        string userId = getConfigValue(user, "userid");
        string groups = getConfigValue(user, "groups");
        string payload = "{'userId': '" + userId + "', 'groups': '" + groups + "'}";
        response.setTextPayload(payload + "\n");
        var responseResult = caller->ok(response);
        if (responseResult is error) {
            log:printError("error responding back to client.", responseResult);
        }
    }

    resource function get data(http:Caller caller, http:Request request) returns @tainted error? {
        http:Response response = new;
        string payload = readFile("./data/data.txt");
        response.setTextPayload("{'data': '" + <@untainted> payload + "'}\n");
        var responseResult = caller->ok(response);
        if (responseResult is error) {
            log:printError("error responding back to client.", responseResult);
        }
    }
}

function getConfigValue(string instanceId, string property) returns (string) {
    string key = <@untainted> instanceId + "." + <@untainted> property;
    return config:getAsString(key, "Invalid User");
}

function readFile(string filePath) returns  string {
    io:ReadableByteChannel bchannel = checkpanic io:openReadableFile(filePath);
    io:ReadableCharacterChannel cChannel = new io:ReadableCharacterChannel(bchannel, "UTF-8");

    var readOutput = cChannel.read(50);
    if (readOutput is string) {
        return <@untainted> readOutput;
    } else {
        return "Error: Unable to read file";
    }
}
