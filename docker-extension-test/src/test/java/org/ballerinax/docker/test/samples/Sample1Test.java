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

package org.ballerinax.docker.test.samples;

import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.test.utils.DockerTestException;
import org.ballerinax.docker.test.utils.DockerTestUtils;
import org.ballerinax.docker.test.utils.ProcessOutput;
import org.ballerinax.docker.utils.DockerPluginUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.ballerinax.docker.generator.DockerGenConstants.ARTIFACT_DIRECTORY;
import static org.ballerinax.docker.test.utils.DockerTestUtils.getExposedPorts;

/**
 * Test class for sample1.
 */
public class Sample1Test extends SampleTest {

    private final Path sourceDirPath = SAMPLE_DIR.resolve("sample1");
    private final Path targetPath = sourceDirPath.resolve(ARTIFACT_DIRECTORY);
    private final String dockerImage = "hello_world_docker:latest";
    private final String dockerContainerName = "ballerinax_docker_" + this.getClass().getSimpleName().toLowerCase();
    private String containerID;

    @BeforeClass
    public void compileSample() throws IOException, InterruptedException {
        Assert.assertEquals(DockerTestUtils.compileBallerinaFile(sourceDirPath, "hello_world_docker.bal"), 0);
    }
    
    @Test(dependsOnMethods = "validateDockerImage", timeOut = 45000)
    public void testService() throws IOException, InterruptedException, DockerTestException {
        containerID = DockerTestUtils.createContainer(dockerImage, dockerContainerName);
        Assert.assertTrue(DockerTestUtils.startContainer(containerID,
                "[ballerina/http] started HTTP/WS listener 0.0.0.0:9090"),
                "Service did not start properly.");
    
        // send request
        ProcessOutput runOutput = DockerTestUtils.runBallerinaFile(CLIENT_BAL_FOLDER, "sample1_client.bal");
        Assert.assertEquals(runOutput.getExitCode(), 0, "Error executing client.");
        Assert.assertEquals(runOutput.getErrOutput().trim(), "", "Unexpected error occurred.");
        Assert.assertEquals(runOutput.getStdOutput(), "Hello, World from service helloWorld ! ",
                "Unexpected service response.");
    }
    
    @Test
    public void validateDockerfile() {
        File dockerFile = new File(targetPath + File.separator + "Dockerfile");
        Assert.assertTrue(dockerFile.exists());
    }

    @Test
    public void validateDockerImage() throws DockerTestException {
        List<String> ports = getExposedPorts(this.dockerImage);
        Assert.assertEquals(ports.size(), 1);
        Assert.assertEquals(ports.get(0), "9090/tcp");
    }

    @AfterClass
    public void cleanUp() throws DockerPluginException, DockerTestException {
        DockerTestUtils.stopContainer(containerID);
        DockerPluginUtils.deleteDirectory(targetPath);
        DockerTestUtils.deleteDockerImage(dockerImage);
    }
}
