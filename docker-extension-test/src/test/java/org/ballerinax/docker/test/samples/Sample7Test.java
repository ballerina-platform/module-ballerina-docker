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
import org.ballerinax.docker.utils.DockerPluginUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.ballerinax.docker.generator.DockerGenConstants.ARTIFACT_DIRECTORY;
import static org.ballerinax.docker.test.utils.DockerTestUtils.getDockerImage;


public class Sample7Test implements SampleTest {

    private final String sourceDirPath = SAMPLE_DIR + File.separator + "sample7";
    private final String targetPath = sourceDirPath + File.separator + ARTIFACT_DIRECTORY;
    private final String dockerImage = "hello_world_docker:latest";

    @BeforeClass
    public void compileSample() throws IOException, InterruptedException {
        Assert.assertEquals(DockerTestUtils.compileBallerinaFile(sourceDirPath, "hello_world_docker.bal"), 0);
    }

    @Test
    public void validateDockerfile() {
        File dockerFile = new File(targetPath + File.separator + "Dockerfile");
        Assert.assertTrue(dockerFile.exists());
    }

    @Test
    public void validateDockerImage() {
        ImageInspect imageInspect = getDockerImage(dockerImage);
        Assert.assertEquals("CMD [\"/bin/sh\" \"-c\" \"ballerina run  hello_world_docker.balx\"]",
                imageInspect.getContainerConfig().getCmd().get(3));
        Assert.assertEquals(2, imageInspect.getContainerConfig().getExposedPorts().size());
        Assert.assertEquals("9090/tcp", imageInspect.getContainerConfig().getExposedPorts().keySet().toArray()[0]);
        Assert.assertEquals("9696/tcp", imageInspect.getContainerConfig().getExposedPorts().keySet().toArray()[1]);
    }

    @AfterClass
    public void cleanUp() throws DockerPluginException {
        DockerPluginUtils.deleteDirectory(targetPath);
        DockerTestUtils.deleteDockerImage(dockerImage);
    }
}
