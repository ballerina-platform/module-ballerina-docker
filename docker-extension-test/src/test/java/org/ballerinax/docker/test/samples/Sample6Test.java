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

import io.fabric8.docker.api.model.ImageInspect;
import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.test.utils.DockerTestUtils;
import org.ballerinax.docker.test.utils.ProcessOutput;
import org.ballerinax.docker.utils.DockerPluginUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.ballerinax.docker.test.utils.DockerTestUtils.getDockerImage;


public class Sample6Test implements SampleTest {

    private final String sourceDirPath = SAMPLE_DIR + File.separator + "sample6";
    private final String targetDirPath = sourceDirPath + File.separator + "target";
    private final String burgerTargetPath = targetDirPath + File.separator + "burger" + File.separator;
    private final String pizzaTargetPath = targetDirPath + File.separator + "pizza" + File.separator;
    private final String burgerDockerImage = "burger:latest";
    private final String burgerContainerName = "ballerinax_docker_burger_" +
                                               this.getClass().getSimpleName().toLowerCase();
    private String burgerContainerID;
    private final String pizzaDockerImage = "pizza:latest";
    private final String pizzaContainerName = "ballerinax_docker_pizza_" +
                                              this.getClass().getSimpleName().toLowerCase();
    private String pizzaContainerID;

    @BeforeClass
    public void compileSample() throws IOException, InterruptedException {
        Assert.assertEquals(DockerTestUtils.compileBallerinaProject(sourceDirPath), 0);
    }
    
    @Test(dependsOnMethods = "validateBurgerDockerImage")
    public void testBurgerService() throws IOException, InterruptedException {
        burgerContainerID = DockerTestUtils.createContainer(burgerDockerImage, burgerContainerName, 9096, 9096);
        Assert.assertTrue(DockerTestUtils.startContainer(burgerContainerID,
                "[ballerina/http] started HTTPS/WSS endpoint 0.0.0.0:9096"),
                "Service did not start properly.");
        
        // send request
        ProcessOutput runOutput = DockerTestUtils.runBallerinaFile(CLIENT_BAL_FOLDER, "sample6_burger_client.bal");
        Assert.assertEquals(runOutput.getExitCode(), 0, "Error executing client.");
        Assert.assertEquals(runOutput.getErrOutput().trim(), "", "Unexpected error occurred.");
        Assert.assertEquals(runOutput.getStdOutput(), "Burger menu ", "Unexpected service response.");
    }
    
    @Test(dependsOnMethods = "validatePizzaDockerImage")
    public void testPizzaService() throws IOException, InterruptedException {
        pizzaContainerID = DockerTestUtils.createContainer(pizzaDockerImage, pizzaContainerName, 9099, 9099);
        Assert.assertTrue(DockerTestUtils.startContainer(pizzaContainerID,
                "[ballerina/http] started HTTP/WS endpoint 0.0.0.0:9099"),
                "Service did not start properly.");
        
        // send request
        ProcessOutput runOutput = DockerTestUtils.runBallerinaFile(CLIENT_BAL_FOLDER, "sample6_pizza_client.bal");
        Assert.assertEquals(runOutput.getExitCode(), 0, "Error executing client.");
        Assert.assertEquals(runOutput.getErrOutput().trim(), "", "Unexpected error occurred.");
        Assert.assertEquals(runOutput.getStdOutput(), "Pizza menu ", "Unexpected service response.");
    }

    @Test
    public void validateBurgerDockerfile() {
        File dockerFile = new File(burgerTargetPath + File.separator + "Dockerfile");
        Assert.assertTrue(dockerFile.exists());
    }

    @Test
    public void validatePizzaDockerfile() {
        File dockerFile = new File(pizzaTargetPath + File.separator + "Dockerfile");
        Assert.assertTrue(dockerFile.exists());
    }

    @Test
    public void validateBurgerDockerImage() {
        ImageInspect imageInspect = getDockerImage(burgerDockerImage);
        Assert.assertEquals(1, imageInspect.getContainerConfig().getExposedPorts().size());
        Assert.assertEquals("9096/tcp", imageInspect.getContainerConfig().getExposedPorts().keySet().toArray()[0]);
    }

    @Test
    public void validatePizzaDockerImage() {
        ImageInspect imageInspect = getDockerImage(pizzaDockerImage);
        Assert.assertEquals(1, imageInspect.getContainerConfig().getExposedPorts().size());
        Assert.assertEquals("9099/tcp", imageInspect.getContainerConfig().getExposedPorts().keySet().toArray()[0]);
    }

    @AfterClass
    public void cleanUp() throws DockerPluginException {
        DockerTestUtils.stopContainer(burgerContainerID);
        DockerTestUtils.stopContainer(pizzaContainerID);
        DockerPluginUtils.deleteDirectory(pizzaTargetPath);
        DockerPluginUtils.deleteDirectory(burgerTargetPath);
        DockerTestUtils.deleteDockerImage(burgerDockerImage);
        DockerTestUtils.deleteDockerImage(pizzaDockerImage);
        DockerPluginUtils.deleteDirectory(sourceDirPath + File.separator + ".ballerina");
        DockerPluginUtils.deleteDirectory(sourceDirPath + File.separator + ".gitignore");
        DockerPluginUtils.deleteDirectory(sourceDirPath + File.separator + "Ballerina.toml");
    }
}
