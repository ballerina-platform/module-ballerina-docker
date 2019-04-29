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
# + name - Name of the docker image. Default value is the file name of the generated balx file.
# + registry - Docker registry url.
# + tag - Docker image tag. Default value is `"latest"`.
# + username - Username for docker registry.
# + password - Password for docker registry.
# + baseImage - Base image to create the docker image. Default value is `"ballerina/ballerina-runtime:<BALLERINA_VERSION>"`.
# Use `"ballerina/ballerina-runtime:latest"` to use the latest stable ballerina runtime docker image.
# + buildImage - Enable building docker image. Default value is `true`.
# + push - Enable pushing docker image to registry. `field buildImage` must be set to `true`. Default value is `false`.
# + enableDebug - Enable ballerina debug. Default is `false`.
# + debugPort - Ballerina remote debug port. Default is `5005`.
# + dockerVersion - Docker API version.
# + dockerHost - Docker host IP and docker PORT. ( e.g minikube IP and docker PORT).
# Default is to use DOCKER_HOST environment variable.
# If DOCKER_HOST is unavailable, use `"unix:///var/run/docker.sock"` for Unix or use `"npipe:////./pipe/docker_engine"` for Windows 10 or use `"localhost:2375"`.
# + dockerCertPath - Docker certificate path. Default is to use `"DOCKER_CERT_PATH"` environment variable.
public type DockerConfiguration record {|
    string name?;
    string registry?;
    string tag?;
    string username?;
    string password?;
    string baseImage?;
    boolean buildImage = true;
    boolean push = false;
    boolean enableDebug = false;
    int debugPort = 5005;
    string dockerVersion?;
    string dockerHost?;
    string dockerCertPath?;
|};

# @docker:Config annotation to configure docker artifact generation.
public annotation<service, listener> Config DockerConfiguration;

# External file type for docker.
#
# + source - Source path of the file (in your machine).
# + target - Target path (inside container).
# + isBallerinaConf - Flag to specify ballerina config file. When true, the config is passed as a command argument to the Dockerfile CMD.
public type FileConfig record {|
    string source;
    string target;
    boolean isBallerinaConf = false;
|};

# External File configurations for docker.
#
# + files - Array of [FileConfig](docker.html#FileConfig)
public type FileConfigs record {|
    FileConfig[] files;
|};

# @docker:CopyFile annotation to copy external files to docker image.
public annotation<service, listener> CopyFiles FileConfigs;

# Expose ports for docker.
public type ExposeConfig record {| |};

# @docker:Expose annotation to expose ballerina ports.
public annotation<listener> Expose ExposeConfig;
