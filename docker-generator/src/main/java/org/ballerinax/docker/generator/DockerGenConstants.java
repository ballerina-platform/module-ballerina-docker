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

package org.ballerinax.docker.generator;

/**
 * Constants used in docker annotation processor.
 */
public class DockerGenConstants {
    public static final String ENABLE_DEBUG_LOGS = "BAL_DOCKER_DEBUG";
    public static final String BALX = ".balx";
    public static final String REGISTRY_SEPARATOR = "/";
    public static final String TAG_SEPARATOR = ":";
    public static final String PORT = "port";
    public static final String ARTIFACT_DIRECTORY = "docker";
    public static final String BALLERINA_BASE_IMAGE = "ballerina/ballerina-runtime";
}
