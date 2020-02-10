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
import java.nio.file.Files;
import java.nio.file.Path;

import static org.ballerinax.docker.generator.DockerGenConstants.ARTIFACT_DIRECTORY;

/**
 * Test class for sample9.
 */
public class Sample10Test extends SampleTest {

    private final Path sourceDirPath = SAMPLE_DIR.resolve("sample10");
    private final Path targetPath = sourceDirPath.resolve(ARTIFACT_DIRECTORY);
    private final String dockerImage = "copy_file_function:latest";
    private final String dockerContainerName = "ballerinax_docker_" + this.getClass().getSimpleName().toLowerCase();
    private String containerID;

    @BeforeClass
    public void compileSample() throws IOException, InterruptedException {
        ProcessOutput buildOutput = DockerTestUtils.compileBallerinaFile(sourceDirPath, "copy_file_function.bal");
        Assert.assertEquals(buildOutput.getExitCode(), 0);
        DockerTestUtils.stopContainer(this.dockerContainerName);
    }

    @Test(timeOut = 45000)
    public void testService() throws IOException, InterruptedException, DockerTestException {
        containerID = DockerTestUtils.createContainer(dockerImage, dockerContainerName);
        Assert.assertTrue(DockerTestUtils.startContainer(containerID,
                "Lorem ipsum dolor sit amet."),
                "Container did not start properly.");
    }

    @Test
    public void validateDockerfile() throws IOException {
        File dockerFile = new File(targetPath + File.separator + "Dockerfile");
        String dockerFileContent = new String(Files.readAllBytes(dockerFile.toPath()));
        Assert.assertTrue(dockerFileContent.contains("adduser -S -s /bin/bash -g 'ballerina' -G troupe -D ballerina"));
        Assert.assertTrue(dockerFileContent.contains("USER ballerina"));
        Assert.assertTrue(dockerFile.exists());
    }

    @AfterClass
    public void cleanUp() throws DockerPluginException {
        DockerTestUtils.stopContainer(containerID);
        DockerPluginUtils.deleteDirectory(targetPath);
        DockerTestUtils.deleteDockerImage(dockerImage);
    }
}
