/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinax.docker.test;

import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.test.utils.DockerTestException;
import org.ballerinax.docker.test.utils.DockerTestUtils;
import org.ballerinax.docker.test.utils.ProcessOutput;
import org.ballerinax.docker.utils.DockerPluginUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.ballerinax.docker.generator.DockerGenConstants.ARTIFACT_DIRECTORY;
import static org.ballerinax.docker.test.utils.DockerTestUtils.getExposedPorts;

/**
 * Generate docker artifacts for modules without @docker annotations.
 */
public class NoAnnotationsModuleTest {
    private static final Path SOURCE_DIR_PATH = Paths.get("src", "test", "resources", "docker-tests",
            "no_annotation_module");
    private static final Path TARGET = SOURCE_DIR_PATH.resolve("target");
    private static final Path CLIENT_BAL_FOLDER = Paths.get("src").resolve("test").resolve("resources")
            .resolve("test_clients").toAbsolutePath();
    private String containerID;
    private String dockerImage;
    
    @Test(timeOut = 90000)
    public void withAndWithoutAnnotationTest() throws IOException, InterruptedException, DockerTestException {
        this.dockerImage = "mix_service:latest";
        String dockerContainerName = "ballerina_docker_mix_" + this.getClass().getSimpleName().toLowerCase();
        
        // Stop if container is already running
        DockerTestUtils.stopContainer(dockerContainerName);
        
        // Compile code
        Assert.assertEquals(DockerTestUtils.compileBallerinaProjectModule(SOURCE_DIR_PATH, "mix_service"), 0);
        
        // Validate docker file
        Path dockerfile = TARGET.resolve(ARTIFACT_DIRECTORY).resolve("mix_service").resolve("Dockerfile");
        String dockerFileContent = new String(Files.readAllBytes(dockerfile));
        Assert.assertTrue(dockerFileContent.contains("adduser -S -s /bin/bash -g 'ballerina' -G troupe -D ballerina"));
        Assert.assertTrue(dockerFileContent.contains("USER ballerina"));
        Assert.assertTrue(dockerfile.toFile().exists());
    
        // Validate expose ports of docker image
        List<String> ports = getExposedPorts(this.dockerImage);
        Assert.assertEquals(ports.size(), 2);
        Assert.assertTrue(ports.contains("9090/tcp"));
        Assert.assertTrue(ports.contains("9091/tcp"));
    
        // Spin up a container
        this.containerID = DockerTestUtils.createContainer(this.dockerImage, dockerContainerName,
                Arrays.asList(9090, 9091));
        Assert.assertTrue(DockerTestUtils.startContainer(this.containerID,
                "[ballerina/http] started HTTP/WS listener 0.0.0.0:9090"),
                "Service did not start properly.");
    
        // Send request to validate
        ProcessOutput runOutput = DockerTestUtils.runBallerinaFile(CLIENT_BAL_FOLDER,
                "hello_world_client_9090_9091.bal");
        Assert.assertEquals(runOutput.getExitCode(), 0, "Error executing client.");
        Assert.assertEquals(runOutput.getStdOutput().trim(),
                "Hello, World from service helloWorld ! Hello, World from service helloWorld !",
                "Unexpected service response.");
    }
    
    @Test(timeOut = 90000)
    public void withoutAnnotationTest() throws IOException, InterruptedException, DockerTestException {
        this.dockerImage = "no_annotations:latest";
        String dockerContainerName = "ballerina_docker_no_annotations_" + this.getClass().getSimpleName().toLowerCase();
        
        // Stop if container is already running
        DockerTestUtils.stopContainer(dockerContainerName);
        
        // Compile code
        Assert.assertEquals(DockerTestUtils.compileBallerinaProjectModule(SOURCE_DIR_PATH, "no_annotations"), 0);
        
        // Validate docker file
        Path dockerfile = TARGET.resolve(ARTIFACT_DIRECTORY).resolve("no_annotations").resolve("Dockerfile");
        String dockerFileContent = new String(Files.readAllBytes(dockerfile));
        Assert.assertTrue(dockerFileContent.contains("adduser -S -s /bin/bash -g 'ballerina' -G troupe -D ballerina"));
        Assert.assertTrue(dockerFileContent.contains("USER ballerina"));
        Assert.assertTrue(dockerfile.toFile().exists());
        
        // Validate expose ports of docker image
        List<String> ports = getExposedPorts(this.dockerImage);
        Assert.assertEquals(ports.size(), 2);
        Assert.assertTrue(ports.contains("9090/tcp"));
        Assert.assertTrue(ports.contains("9091/tcp"));
        
        // Spin up a container
        this.containerID = DockerTestUtils.createContainer(this.dockerImage, dockerContainerName,
                Arrays.asList(9090, 9091));
        Assert.assertTrue(DockerTestUtils.startContainer(this.containerID,
                "[ballerina/http] started HTTP/WS listener 0.0.0.0:9090"),
                "Service did not start properly.");
        
        // Send request to validate
        ProcessOutput runOutput = DockerTestUtils.runBallerinaFile(CLIENT_BAL_FOLDER,
                "hello_world_client_9090_9091.bal");
        Assert.assertEquals(runOutput.getExitCode(), 0, "Error executing client.");
        Assert.assertEquals(runOutput.getStdOutput().trim(),
                "Hello, World from service helloWorld ! Hello, World from service helloWorld !",
                "Unexpected service response.");
    }
    
    @AfterMethod
    public void cleanUp() throws DockerPluginException {
        DockerTestUtils.stopContainer(this.containerID);
        DockerPluginUtils.deleteDirectory(TARGET);
        DockerTestUtils.deleteDockerImage(this.dockerImage);
    }
}
