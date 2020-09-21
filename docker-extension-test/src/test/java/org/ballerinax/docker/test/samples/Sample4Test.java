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
import static org.ballerinax.docker.generator.DockerGenConstants.MODULE_INIT_QUOTED;
import static org.ballerinax.docker.test.utils.DockerTestUtils.getCommand;
import static org.ballerinax.docker.test.utils.DockerTestUtils.getExposedPorts;

/**
 * Test class for sample4.
 */
public class Sample4Test extends SampleTest {

    private final Path sourceDirPath = SAMPLE_DIR.resolve("sample4");
    private final Path targetPath = sourceDirPath.resolve(ARTIFACT_DIRECTORY);
    private final String dockerImage = "helloworld-debug:latest";
    private final String dockerContainerName = "ballerinax_docker_" + this.getClass().getSimpleName().toLowerCase();
    private String containerID;

    @BeforeClass
    public void compileSample() throws IOException, InterruptedException {
        Assert.assertEquals(DockerTestUtils.compileBallerinaFile(sourceDirPath, "docker_debug.bal").getExitCode(), 0);
        DockerTestUtils.stopContainer(this.dockerContainerName);
    }

    @Test(dependsOnMethods = "validateDockerImage", timeOut = 45000)
    public void testService() throws DockerTestException {
        containerID = DockerTestUtils.createContainer(dockerImage, dockerContainerName);
        Assert.assertTrue(DockerTestUtils.startContainer(containerID, "Listening for transport dt_socket at address: " +
                "5005"), "Service did not start properly.");
    }

    @Test
    public void validateDockerfile() {
        File dockerFile = new File(targetPath + File.separator + "Dockerfile");
        Assert.assertTrue(dockerFile.exists());
    }

    @Test
    public void validateDockerImage() {
        List<String> ports = getExposedPorts(this.dockerImage);
        Assert.assertEquals(ports.size(), 2);
        Assert.assertEquals(ports.get(0), "5005/tcp");
        Assert.assertEquals(ports.get(1), "9090/tcp");
        List<String> commands = getCommand(this.dockerImage);
        Assert.assertEquals(commands.size(), 3);
        Assert.assertEquals(commands.get(2), "java -Xdebug -Xnoagent -Djava.compiler=NONE " +
                "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005-Xdiag -cp \"docker_debug.jar:jars/*\" " +
                MODULE_INIT_QUOTED);
    }

    @AfterClass
    public void cleanUp() throws DockerPluginException {
        DockerTestUtils.stopContainer(containerID);
        DockerPluginUtils.deleteDirectory(targetPath);
        DockerTestUtils.deleteDockerImage(dockerImage);
    }
}
