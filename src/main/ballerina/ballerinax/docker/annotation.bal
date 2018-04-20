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

documentation {Docker annotation configuration
    F{{name}} - Name of the docker image
    F{{registry}} - Docker registry url
    F{{tag}} - Docker registry url
    F{{username}} - Docker registry username
    F{{password}} - Docker registry password
    F{{baseImage}} - Base image for Dockerfile
    F{{push}} - Enable pushing docker image to registry
    F{{buildImage}} - Enable docker image build
    F{{enableDebug}} - Enable ballerina debug
    F{{debugPort}} - Ballerina debug port
    F{{dockerHost}} - Docker host IP and docker PORT. ( e.g minikube IP and docker PORT)
    F{{dockerCertPath}} - Docker certificate path
}
public type DockerConfiguration {
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

documentation {@docker:Config annotation to configure docker artifact generation
}
public annotation < service, endpoint > Config DockerConfiguration;

documentation {External file type for docker
    F{{source}} - source path of the file (in your machine)
    F{{target}} - target path (inside container)
    F{{isBallerinaConf}} - Flag to specify ballerina config file
}
public type FileConfig {
    string source;
    string target;
    boolean isBallerinaConf;
};

documentation {External File configurations for docker
    F{{files}} - Array of [FileConfig](docker.html#FileConfig)
}
public type FileConfigs {
    FileConfig[] files;
};

documentation {@docker:CopyFile annotation to copy external files to docker image
}
public annotation < service, endpoint > CopyFiles FileConfigs;

documentation {Expose ports for docker
}
public type ExposeConfig {
};


documentation {@docker:Expose annotation to expose ballerina ports
}
public annotation < endpoint > Expose ExposeConfig;
