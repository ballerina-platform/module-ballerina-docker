/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.artifactgen;

/**
 * Constants used in docker annotation processor.
 */
public class DockerGenConstants {
    public static final String ENABLE_DEBUG_LOGS = "debugDocker";

    // Annotation package constants
    public static final String DOCKER_ANNOTATION_PACKAGE = "ballerina.docker";
    public static final String DOCKER_ANNOTATION = "configuration";

    //Docker annotation constants
    public static final String DOCKER_NAME = "name";
    public static final String DOCKER_REGISTRY = "registry";
    public static final String DOCKER_TAG = "tag";
    public static final String DOCKER_USERNAME = "username";
    public static final String DOCKER_PASSWORD = "password";
    public static final String DOCKER_PUSH = "push";
    public static final String DOCKER_TAG_LATEST = "latest";
    public static final String DOCKER_IMAGE_BUILD = "buildImage";
    public static final String DOCKER_DEBUG_ENABLE = "debugEnable";
    public static final String DOCKER_DEBUG_PORT = "debugPort";
    public static final String DOCKER_BASE_IMAGE = "baseImage";

}
