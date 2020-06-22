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
import org.ballerinax.docker.test.samples.SampleTest;
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
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.ballerinax.docker.test.utils.DockerTestUtils.getExposedPorts;

/**
 * Test class for Thin jar creation from Ballerina Project.
 */
public class ThinJarTest extends SampleTest {

    private final Path sourceDirPath = Paths.get("src", "test", "resources", "docker-tests", "thin_jar");
    private final Path targetDirPath = sourceDirPath.resolve("target");
    private final Path burgerTargetPath = targetDirPath.resolve("docker").resolve("burger");
    private final Path pizzaTargetPath = targetDirPath.resolve("docker").resolve("pizza");
    private final String burgerDockerImage = "foody-burger-0.0.1:latest";
    private final String burgerContainerName = "ballerinax_docker_burger_" +
            this.getClass().getSimpleName().toLowerCase();
    private final String pizzaDockerImage = "foody-pizza-0.0.1:latest";
    private final String pizzaContainerName = "ballerinax_docker_pizza_" +
            this.getClass().getSimpleName().toLowerCase();
    private String burgerContainerID;
    private String pizzaContainerID;

    @BeforeClass
    public void compileSample() throws IOException, InterruptedException {
        Assert.assertEquals(DockerTestUtils.compileBallerinaProject(sourceDirPath), 0);
        DockerTestUtils.stopContainer(this.burgerContainerName);
        DockerTestUtils.stopContainer(this.pizzaContainerName);
    }

    @Test(dependsOnMethods = "validateBurgerDockerImage", timeOut = 45000)
    public void testBurgerService() throws IOException, InterruptedException, DockerTestException {
        burgerContainerID = DockerTestUtils.createContainer(burgerDockerImage, burgerContainerName,
                Collections.singletonList(9096));
        Assert.assertTrue(DockerTestUtils.startContainer(burgerContainerID,
                "[ballerina/http] started HTTPS/WSS listener 0.0.0.0:9096"),
                "Service did not start properly.");

        // send request
        ProcessOutput runOutput = DockerTestUtils.runBallerinaFile(CLIENT_BAL_FOLDER, "sample6_burger_client.bal");
        Assert.assertEquals(runOutput.getExitCode(), 0, "Error executing client.");
        Assert.assertEquals(runOutput.getStdOutput(), "Burger menu ", "Unexpected service response.");
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
    public void validateBurgerDockerfile() {
        File dockerFile = burgerTargetPath.resolve("Dockerfile").toFile();
        Assert.assertTrue(dockerFile.exists());
    }

    @Test
    public void validatePizzaDockerfile() {
        File dockerFile = pizzaTargetPath.resolve("Dockerfile").toFile();
        Assert.assertTrue(dockerFile.exists());
    }

    @Test
    public void validateBurgerDockerImage() {
        List<String> ports = getExposedPorts(burgerDockerImage);
        Assert.assertEquals(ports.size(), 1);
        Assert.assertEquals(ports.get(0), "9096/tcp");
    }

    @Test
    public void validatePizzaDockerImage() {
        List<String> ports = getExposedPorts(pizzaDockerImage);
        Assert.assertEquals(ports.size(), 1);
        Assert.assertEquals(ports.get(0), "9099/tcp");
    }

    @AfterClass
    public void cleanUp() throws DockerPluginException {
        DockerTestUtils.stopContainer(burgerContainerID);
        DockerTestUtils.stopContainer(pizzaContainerID);
        DockerPluginUtils.deleteDirectory(pizzaTargetPath);
        DockerPluginUtils.deleteDirectory(burgerTargetPath);
        DockerTestUtils.deleteDockerImage(burgerDockerImage);
        DockerTestUtils.deleteDockerImage(pizzaDockerImage);
    }
}
