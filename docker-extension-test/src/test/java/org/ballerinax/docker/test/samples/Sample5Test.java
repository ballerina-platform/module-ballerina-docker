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
import java.util.List;

import static org.ballerinax.docker.generator.DockerGenConstants.ARTIFACT_DIRECTORY;
import static org.ballerinax.docker.test.utils.DockerTestUtils.getCommand;
import static org.ballerinax.docker.test.utils.DockerTestUtils.getExposedPorts;

/**
 * Test class for sample5.
 */
public class Sample5Test implements SampleTest {

    private final String sourceDirPath = SAMPLE_DIR + File.separator + "sample5";
    private final String targetPath = sourceDirPath + File.separator + ARTIFACT_DIRECTORY;
    private final String dockerImage = "hello_config_file:latest";
    private final String dockerContainerName = "ballerinax_docker_" + this.getClass().getSimpleName().toLowerCase();
    private String containerID;

    @BeforeClass
    public void compileSample() throws IOException, InterruptedException {
        Assert.assertEquals(DockerTestUtils.compileBallerinaFile(sourceDirPath, "hello_config_file.bal"), 0);
    }
    
    @Test(dependsOnMethods = "validateDockerImage", timeOut = 30000)
    public void testService() throws IOException, InterruptedException, DockerTestException {
        containerID = DockerTestUtils.createContainer(dockerImage, dockerContainerName);
        Assert.assertTrue(DockerTestUtils.startContainer(containerID,
                "[ballerina/http] started HTTP/WS endpoint 0.0.0.0:9090"),
                "Service did not start properly.");
        
        // send request
        ProcessOutput runOutput = DockerTestUtils.runBallerinaFile(CLIENT_BAL_FOLDER, "sample5_client.bal");
        Assert.assertEquals(runOutput.getExitCode(), 0, "Error executing client.");
        Assert.assertEquals(runOutput.getErrOutput().trim(), "", "Unexpected error occurred.");
        Assert.assertTrue(runOutput.getStdOutput().contains("{'userId': 'john@ballerina.com', 'groups': 'apim,esb'}"),
                "Unexpected service response.");
        Assert.assertTrue(runOutput.getStdOutput().contains("{'userId': 'jane3@ballerina.com', 'groups': 'esb'}"),
                "Unexpected service response.");
        Assert.assertTrue(runOutput.getStdOutput().contains("{'data': 'Lorem ipsum dolor sit amet.'}"),
                "Unexpected service response.");
    }

    @Test
    public void validateDockerfile() {
        File dockerFile = new File(targetPath + File.separator + "Dockerfile");
        Assert.assertTrue(dockerFile.exists());
    }
    
    @Test
    public void validateDockerImage() throws DockerTestException {
        Assert.assertEquals(getCommand(this.dockerImage).toString(), "[/bin/sh, -c, ballerina run  " +
                                                                     "--config /home/ballerina/conf/ballerina.conf " +
                                                                     "hello_config_file.balx]");
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
