/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
public class Sample8Test extends SampleTest {

    private final Path sourceDirPath = SAMPLE_DIR.resolve("sample8");
    private final Path targetPath = sourceDirPath.resolve(ARTIFACT_DIRECTORY);
    private final String dockerImageThin = "cmd_thin_jar:latest";
    private final String dockerContainerName = "ballerinax_docker_thin";

    @BeforeClass
    public void compileSample() throws IOException, InterruptedException {
        Assert.assertEquals(
                DockerTestUtils.compileBallerinaFile(sourceDirPath, "cmd_thin_jar.bal").getExitCode(), 0);
        DockerTestUtils.stopContainer(this.dockerContainerName);
    }

    @Test(dependsOnMethods = "validateDockerImage", timeOut = 45000)
    public void testService() throws IOException, InterruptedException, DockerTestException {
        String containerID = DockerTestUtils.createContainer(dockerImageThin, dockerContainerName);
        Assert.assertTrue(DockerTestUtils.startContainer(containerID,
                "[ballerina/http] started HTTP/WS listener 0.0.0.0:9090"),
                "Service did not start properly.");

        // send request
        ProcessOutput runOutput = DockerTestUtils.runBallerinaFile(CLIENT_BAL_FOLDER, "sample8_client.bal");
        Assert.assertEquals(runOutput.getExitCode(), 0, "Error executing client.");
        Assert.assertEquals(runOutput.getStdOutput(), "Hello, World from service helloWorld ! ",
                "Unexpected service response.");
        DockerTestUtils.stopContainer(containerID);
    }


    @Test
    public void validateDockerfile() {
        File dockerFile = new File(targetPath + File.separator + "Dockerfile");
        Assert.assertTrue(dockerFile.exists());
    }

    @Test
    public void validateDockerImage() {
        List<String> ports = getExposedPorts(this.dockerImageThin);
        Assert.assertEquals(ports.size(), 1);
        Assert.assertEquals(ports.get(0), "9090/tcp");
    }

    @AfterClass
    public void cleanUp() throws DockerPluginException {
        DockerPluginUtils.deleteDirectory(targetPath);
        DockerTestUtils.stopContainer(this.dockerContainerName);
        DockerTestUtils.deleteDockerImage(dockerImageThin);
    }
}
