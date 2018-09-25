// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# Docker annotation configuration.
#
# + name - Name of the docker image
# + registry - Docker registry url
# + tag - Docker image tag
# + username - Docker registry username
# + password - Docker registry password
# + baseImage - Base image for Dockerfile
# + push - Enable pushing docker image to registry
# + buildImage - Enable docker image build
# + enableDebug - Enable ballerina debug
# + debugPort - Ballerina debug port
# + dockerHost - Docker host IP and docker PORT. ( e.g minikube IP and docker PORT)
# + dockerCertPath - Docker certificate path
public type DockerConfiguration record {
    string name;
    string registry;
    string tag;
    string username;
    string password;
    string baseImage;
    boolean push;
    boolean buildImage;
    boolean enableDebug;
    int debugPort;
    string dockerHost;
    string dockerCertPath;
};

# @docker:Config annotation to configure docker artifact generation.
public annotation<service, endpoint> Config DockerConfiguration;

# External file type for docker.
#
# + source - source path of the file (in your machine)
# + target - target path (inside container)
# + isBallerinaConf - Flag to specify ballerina config file
public type FileConfig record {
    string source;
    string target;
    boolean isBallerinaConf;
};

# External File configurations for docker.
#
# + files - Array of [FileConfig](docker.html#FileConfig)
public type FileConfigs record {
    FileConfig[] files;
};

# @docker:CopyFile annotation to copy external files to docker image.
public annotation<service, endpoint> CopyFiles FileConfigs;

# Expose ports for docker.
public type ExposeConfig record {
};


# @docker:Expose annotation to expose ballerina ports.
public annotation<endpoint> Expose ExposeConfig;
