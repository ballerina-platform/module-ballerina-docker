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

package org.ballerinax.docker;

/**
 * Constants used in docker annotation processor.
 */
public class DockerGenConstants {
    public static final String ENABLE_DEBUG_LOGS = "debugDocker";
    public static final String BALX = ".balx";
    public static final String REGISTRY_SEPARATOR = "/";
    public static final String TAG_SEPARATOR = ":";
    public static final String PORT = "port";
    public static final String ARTIFACT_DIRECTORY = "docker";
    public static final String UNIX_DEFAULT_DOCKER_HOST = "unix:///var/run/docker.sock";
    public static final String WINDOWS_DEFAULT_DOCKER_HOST = "tcp://localhost:2375";
    static final String DOCKER_HOST = "DOCKER_HOST";
    static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";
}
