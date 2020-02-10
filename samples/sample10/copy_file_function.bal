// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/config;
import ballerina/io;
import ballerina/docker;

@docker:Config {}
@docker:CopyFiles {
    files: [
        { sourceFile: "./conf/ballerina.conf", target: "/home/ballerina/conf/ballerina.conf", isBallerinaConf: true },
        { sourceFile: "./conf/data.txt", target: "/home/ballerina/data/data.txt" }
    ]
}
public function main() {
    string userId = getConfigValue("jane", "userid");
    string groups = getConfigValue("jane", "groups");
    string config = "{'userId': '" + userId + "', 'groups': '" + groups + "'}";
    io:println(config);
    string data = readFile("./data/data.txt");
    io:println(data);
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
