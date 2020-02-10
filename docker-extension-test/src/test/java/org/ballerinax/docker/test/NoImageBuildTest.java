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
import java.nio.file.Paths;

import static org.ballerinax.docker.generator.DockerGenConstants.ARTIFACT_DIRECTORY;

/**
 * Build with `buildImage` field set to false and check whether build command is shown.
 */
public class NoImageBuildTest {
    private final Path sourceDirPath = Paths.get("src", "test", "resources", "docker-tests");
    private final Path targetPath = sourceDirPath.resolve(ARTIFACT_DIRECTORY);
    
    @BeforeClass
    public void compileSample() throws IOException, InterruptedException {
        ProcessOutput buildProcess = DockerTestUtils.compileBallerinaFile(sourceDirPath, "build_image_false.bal");
        Assert.assertEquals(buildProcess.getExitCode(), 0);
    }
    
    @Test
    public void validateDockerfile() throws IOException {
        File dockerFile = this.targetPath.resolve("Dockerfile").toFile();
        Assert.assertTrue(dockerFile.exists());
        String dockerFileContent = new String(Files.readAllBytes(dockerFile.toPath()));
        Assert.assertTrue(dockerFileContent.contains("adduser -S -s /bin/bash -g 'ballerina' -G troupe -D ballerina"));
        Assert.assertTrue(dockerFileContent.contains("USER ballerina"));
    }
    
    @AfterClass
    public void cleanUp() throws DockerPluginException {
        DockerPluginUtils.deleteDirectory(targetPath);
    }
}
