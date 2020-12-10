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

import ballerina/http;
import ballerina/log;
import ballerina/docker;

@docker:Config {
    cmd: "CMD java -Xdiag -cp '${APP}:jars/*' '$_init' --b7a.http.accesslog.console=true"
}
service http:Service /helloWorld on new http:Listener(9090) {
    resource function get sayHello(http:Caller caller) {
        var responseResult = caller->ok("Hello, World from service helloWorld ! \n");
        if (responseResult is error) {
            log:printError("error responding back to client.", responseResult);
        }
    }
}
