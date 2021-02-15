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
import java.util.Collections;
import java.util.List;

import static org.ballerinax.docker.test.utils.DockerTestUtils.getExposedPorts;

/**
 * Test class for sample6.
 */
public class Sample6Test extends SampleTest {

    private final Path sourceDirPath = SAMPLE_DIR.resolve("sample6");
    private final Path targetDirPath = sourceDirPath.resolve("target");
    private final Path pizzaTargetPath = targetDirPath.resolve("docker").resolve("pizza-0.0.1");
    private final String pizzaDockerImage = "pizza-0.0.1:latest";
    private final String pizzaContainerName = "ballerinax_docker_pizza_" +
            this.getClass().getSimpleName().toLowerCase();
    private String pizzaContainerID;

    @BeforeClass
    public void compileSample() throws IOException, InterruptedException {
        Assert.assertEquals(DockerTestUtils.compileBallerinaProject(sourceDirPath), 0);
        DockerTestUtils.stopContainer(this.pizzaContainerName);
    }


    @Test(dependsOnMethods = "validatePizzaDockerImage", timeOut = 45000)
    public void testPizzaService() throws IOException, InterruptedException, DockerTestException {
        pizzaContainerID = DockerTestUtils.createContainer(pizzaDockerImage, pizzaContainerName,
                Collections.singletonList(9099));
        Assert.assertTrue(DockerTestUtils.startContainer(pizzaContainerID,
                "[ballerina/http] started HTTP/WS listener 0.0.0.0:9099"),
                "Service did not start properly.");

        // send request
        ProcessOutput runOutput = DockerTestUtils.runBallerinaFile(CLIENT_BAL_FOLDER, "sample6_pizza_client.bal");
        Assert.assertEquals(runOutput.getExitCode(), 0, "Error executing client.");
        Assert.assertEquals(runOutput.getStdOutput(), "Pizza menu ", "Unexpected service response.");
    }

    @Test
    public void validatePizzaDockerfile() {
        File dockerFile = pizzaTargetPath.resolve("Dockerfile").toFile();
        Assert.assertTrue(dockerFile.exists());
    }

    @Test
    public void validatePizzaDockerImage() {
        List<String> ports = getExposedPorts(pizzaDockerImage);
        Assert.assertEquals(ports.size(), 1);
        Assert.assertEquals(ports.get(0), "9099/tcp");
    }

    @AfterClass
    public void cleanUp() throws DockerPluginException {
        DockerTestUtils.stopContainer(pizzaContainerID);
        DockerPluginUtils.deleteDirectory(pizzaTargetPath);
        DockerTestUtils.deleteDockerImage(pizzaDockerImage);
    }
}
