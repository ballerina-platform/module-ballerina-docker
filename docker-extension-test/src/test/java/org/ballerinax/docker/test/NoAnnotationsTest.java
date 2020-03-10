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
import java.util.List;

import static org.ballerinax.docker.generator.DockerGenConstants.ARTIFACT_DIRECTORY;
import static org.ballerinax.docker.test.utils.DockerTestUtils.getExposedPorts;

/**
 * Generate docker artifacts without @docker annotations.
 */
public class NoAnnotationsTest {
    private static final Path SOURCE_DIR_PATH = Paths.get("src", "test", "resources", "docker-tests");
    private static final Path TARGET_PATH = SOURCE_DIR_PATH.resolve(ARTIFACT_DIRECTORY);
    private static final Path CLIENT_BAL_FOLDER = Paths.get("src").resolve("test").resolve("resources")
            .resolve("test_clients").toAbsolutePath();
    private String containerID;
    private String dockerImage;
    private String jarName;
    
    @Test(timeOut = 90000)
    public void serviceWithNoAnnotationTest() throws IOException, InterruptedException, DockerTestException {
        this.dockerImage = "no_annotation_service:latest";
        this.jarName = "no_annotation_service.jar";
        String dockerContainerName = "ballerina_docker_svc_" + this.getClass().getSimpleName().toLowerCase();
        
        // Stop if container is already running
        DockerTestUtils.stopContainer(dockerContainerName);
        
        // Compile code
        Assert.assertEquals(DockerTestUtils.compileBallerinaFile(SOURCE_DIR_PATH, "no_annotation_service.bal")
                .getExitCode(), 0);
    
        // Validate docker file
        Path dockerfile = TARGET_PATH.resolve("Dockerfile");
        String dockerFileContent = new String(Files.readAllBytes(dockerfile));
        Assert.assertTrue(dockerFileContent.contains("adduser -S -s /bin/bash -g 'ballerina' -G troupe -D ballerina"));
        Assert.assertTrue(dockerFileContent.contains("USER ballerina"));
        Assert.assertTrue(dockerfile.toFile().exists());
    
        // Validate expose ports of docker image
        List<String> ports = getExposedPorts(this.dockerImage);
        Assert.assertEquals(ports.size(), 1);
        Assert.assertEquals(ports.get(0), "9090/tcp");
    
        // Spin up a container
        this.containerID = DockerTestUtils.createContainer(this.dockerImage, dockerContainerName);
        Assert.assertTrue(DockerTestUtils.startContainer(this.containerID,
                "[ballerina/http] started HTTP/WS listener 0.0.0.0:9090"),
                "Service did not start properly.");
    
        // Send request to validate
        ProcessOutput runOutput = DockerTestUtils.runBallerinaFile(CLIENT_BAL_FOLDER, "hello_world_client.bal");
        Assert.assertEquals(runOutput.getExitCode(), 0, "Error executing client.");
        Assert.assertEquals(runOutput.getStdOutput(), "Hello, World!", "Unexpected service response.");
    }
    
    @Test(timeOut = 90000)
    public void listenerWithNoAnnotationTest() throws IOException, InterruptedException, DockerTestException {
        this.dockerImage = "no_annotation_listener:latest";
        this.jarName = "no_annotation_listener.jar";
        String dockerContainerName = "ballerina_docker_lstnr_" + this.getClass().getSimpleName().toLowerCase();
        
        // Stop if container is already running
        DockerTestUtils.stopContainer(dockerContainerName);
        
        // Compile code
        Assert.assertEquals(DockerTestUtils.compileBallerinaFile(SOURCE_DIR_PATH, "no_annotation_listener.bal")
                .getExitCode(), 0);
        
        // Validate docker file
        Path dockerfile = TARGET_PATH.resolve("Dockerfile");
        String dockerFileContent = new String(Files.readAllBytes(dockerfile));
        Assert.assertTrue(dockerFileContent.contains("adduser -S -s /bin/bash -g 'ballerina' -G troupe -D ballerina"));
        Assert.assertTrue(dockerFileContent.contains("USER ballerina"));
        Assert.assertTrue(dockerfile.toFile().exists());
        
        // Validate expose ports of docker image
        List<String> ports = getExposedPorts(this.dockerImage);
        Assert.assertEquals(ports.size(), 1);
        Assert.assertEquals(ports.get(0), "9090/tcp");
        
        // Spin up a container
        this.containerID = DockerTestUtils.createContainer(this.dockerImage, dockerContainerName);
        Assert.assertTrue(DockerTestUtils.startContainer(this.containerID,
                "[ballerina/http] started HTTP/WS listener 0.0.0.0:9090"),
                "Service did not start properly.");
        
        // Send request to validate
        ProcessOutput runOutput = DockerTestUtils.runBallerinaFile(CLIENT_BAL_FOLDER, "hello_world_client.bal");
        Assert.assertEquals(runOutput.getExitCode(), 0, "Error executing client.");
        Assert.assertEquals(runOutput.getStdOutput().trim(), "Hello, World!", "Unexpected service response.");
    }
    
    @Test(timeOut = 90000)
    public void mainWithNoAnnotationTest() throws IOException, InterruptedException, DockerTestException {
        this.dockerImage = "no_annotation_main:latest";
        this.jarName = "no_annotation_main.jar";
        String dockerContainerName = "ballerina_docker_main_" + this.getClass().getSimpleName().toLowerCase();
        
        // Stop if container is already running
        DockerTestUtils.stopContainer(dockerContainerName);
        
        // Compile code
        Assert.assertEquals(DockerTestUtils.compileBallerinaFile(SOURCE_DIR_PATH, "no_annotation_main.bal")
                .getExitCode(), 0);
        
        // Validate docker file
        Path dockerfile = TARGET_PATH.resolve("Dockerfile");
        String dockerFileContent = new String(Files.readAllBytes(dockerfile));
        Assert.assertTrue(dockerFileContent.contains("adduser -S -s /bin/bash -g 'ballerina' -G troupe -D ballerina"));
        Assert.assertTrue(dockerFileContent.contains("USER ballerina"));
        Assert.assertTrue(dockerfile.toFile().exists());
        
        // Validate expose ports of docker image
        List<String> ports = getExposedPorts(this.dockerImage);
        Assert.assertEquals(ports.size(), 0);
        
        // Spin up a container
        this.containerID = DockerTestUtils.createContainer(this.dockerImage, dockerContainerName);
        Assert.assertTrue(DockerTestUtils.startContainer(this.containerID, "Hello, World!"),
                "Main function did not run.");
    }
    
    @AfterMethod
    public void cleanUp() throws DockerPluginException, IOException {
        DockerTestUtils.stopContainer(this.containerID);
        DockerPluginUtils.deleteDirectory(TARGET_PATH);
        DockerTestUtils.deleteDockerImage(this.dockerImage);
        Files.deleteIfExists(SOURCE_DIR_PATH.resolve(this.jarName));
    }
}
