import ballerina/net.http;
import ballerina/io;
import ballerinax/docker;

@docker:Config {}
@docker:CopyFiles{
    files:[
            {source:"./conf/sample.toml",target:"/home/ballerina/conf/sample.toml",isBallerinaConf:true},
            {source:"./conf/data.txt", target:"/home/ballerina/data/data.txt"}
          ]
}
endpoint http:ServiceEndpoint helloWorldEP {
    port:9090
};

@http:ServiceConfig {
      basePath:"/helloWorld"
}
service<http:Service> helloWorld bind helloWorldEP {
  	@http:ResourceConfig {
        methods:["GET"],
        path:"/config"
    }
    getConfig (endpoint outboundEP, http:Request request) {
        http:Response response = {};
        string payload = readFile("./conf/sample.toml", "r", "UTF-8");
        response.setStringPayload("Configs: "+ payload +"\n");
        _ = outboundEP -> respond(response);
    }
    @http:ResourceConfig {
        methods:["GET"],
        path:"/data"
    }
    getData (endpoint outboundEP, http:Request request) {
        http:Response response = {};
        string payload = readFile("./data/data.txt", "r", "UTF-8");
        response.setStringPayload("Data: "+ payload +"\n");
        _ = outboundEP -> respond(response);
    }
}

function readFile (string filePath, string permission, string encoding) returns (string) {
    io:ByteChannel channel = io:openFile(filePath, permission);
    var characterChannelResult = io:createCharacterChannel(channel, encoding);
    io:CharacterChannel sourceChannel={};
    match characterChannelResult {  
        (io:CharacterChannel) res => {
            sourceChannel = res;
        }
        error err => {
            io:println(err);
        }
    }
    var contentResult = sourceChannel.readCharacters(50);
    match contentResult {
        (string) res => {
            return res;
        }
        error err => {
            io:println(err);
            return err.message;
        }
    }    
}

