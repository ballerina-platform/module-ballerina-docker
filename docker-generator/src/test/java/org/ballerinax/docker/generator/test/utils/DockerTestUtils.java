/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinax.docker.generator.test.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Docker test utils.
 */
public class DockerTestUtils {

    private static final Logger log = LoggerFactory.getLogger(DockerTestUtils.class);

    public static DockerClient getDockerClient() {
        DefaultDockerClientConfig.Builder dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder();
        return DockerClientBuilder.getInstance(dockerClientConfig.build()).build();
    }

    /**
     * Return a ImageInspect object for a given Docker Image name.
     *
     * @param imageName Docker image Name
     * @return ImageInspect object
     */
    public static InspectImageResponse getDockerImage(String imageName) {
        return getDockerClient().inspectImageCmd(imageName).exec();
    }

    /**
     * Get the list of exposed ports of the docker image.
     *
     * @param imageName The docker image name.
     * @return Exposed ports.
     */
    public static List<String> getExposedPorts(String imageName) {
        InspectImageResponse dockerImage = getDockerImage(imageName);
        if (null == dockerImage.getConfig()) {
            return new ArrayList<>();
        }

        ExposedPort[] exposedPorts = dockerImage.getConfig().getExposedPorts();
        return Arrays.stream(exposedPorts).map(ExposedPort::toString).collect(Collectors.toList());
    }

    /**
     * Get the list of commands of the docker image.
     *
     * @param imageName The docker image name.
     * @return The list of commands.
     */
    public static List<String> getCommand(String imageName) {
        InspectImageResponse dockerImage = getDockerImage(imageName);
        if (null == dockerImage.getConfig() || null == dockerImage.getConfig().getCmd()) {
            return new ArrayList<>();
        }

        return Arrays.asList(dockerImage.getConfig().getCmd());
    }

    /**
     * Delete a given Docker image and prune.
     *
     * @param imageName Docker image Name
     */
    public static void deleteDockerImage(String imageName) {
        getDockerClient().removeImageCmd(imageName).withForce(true).withNoPrune(false).exec();
    }

}
