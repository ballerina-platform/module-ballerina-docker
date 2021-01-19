// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/io;
import ballerina/http;

public function main(string... args) {
    args = <@untainted> args;
    testEndpoint(args[0]);
    testSecuredEndpoint(args[0]);
}

function testEndpoint(string host) {
    http:Client helloWorldEP = checkpanic new("http://" + host + ":9090");

    var response = helloWorldEP->get("/helloWorld/sayHello");
    if (response is http:Response) {
        io:println(response.getTextPayload());
    } else {
        io:println(response);
    }
}

function testSecuredEndpoint(string host) {
    http:Client helloWorldSecuredEP = checkpanic new("https://" + host + ":9696", {
            secureSocket: {
                trustStore: {
                    path: "security/ballerinaTruststore.p12",
                    password: "ballerina"
                },
                verifyHostname: false
            }
        });

    var response = helloWorldSecuredEP->get("/helloWorld/sayHello");
    if (response is http:Response) {
        io:println(response.getTextPayload());
    } else {
        io:println(response);
    }
}
