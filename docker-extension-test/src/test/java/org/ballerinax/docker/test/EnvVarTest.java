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
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ballerinax.docker.generator.DockerGenConstants.ARTIFACT_DIRECTORY;
import static org.ballerinax.docker.test.utils.DockerTestUtils.getExposedPorts;

/**
 * Build with `buildImage` field set to false and check whether build command is shown.
 */
public class EnvVarTest {
    private final Path sourceDirPath = Paths.get("src", "test", "resources", "docker-tests");
    private final Path targetPath = sourceDirPath.resolve(ARTIFACT_DIRECTORY);
    private final String dockerImage = "env:latest";

    private void setEnv(Map<String, String> newEnv) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newEnv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField(
                    "theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> ciEnv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            ciEnv.putAll(newEnv);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newEnv);
                }
            }
        }
    }

    @BeforeClass
    public void compileSample() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("PASSWORD", "myP@$$worD");
        setEnv(env);
        ProcessOutput buildProcess = DockerTestUtils.compileBallerinaFile(sourceDirPath, "env.bal");
        Assert.assertEquals(buildProcess.getExitCode(), 0);
    }

    @Test
    public void validateDockerfile() throws IOException {
        File dockerFile = new File(targetPath + File.separator + "Dockerfile");
        Assert.assertTrue(dockerFile.exists());
        String dockerFileContent = new String(Files.readAllBytes(dockerFile.toPath()));
        Assert.assertTrue(dockerFileContent.contains("ENV testVar1=value1\n" +
                "ENV testVar2=value2\nENV secret=myP@$$worD"));
        Assert.assertTrue(dockerFileContent.contains("USER ballerina"));
    }

    @Test
    public void validateDockerImage() {
        List<String> ports = getExposedPorts(this.dockerImage);
        Assert.assertEquals(ports.size(), 1);
        Assert.assertEquals(ports.get(0), "9090/tcp");
    }

    @AfterClass
    public void cleanUp() throws DockerPluginException {
        DockerPluginUtils.deleteDirectory(targetPath);
        DockerTestUtils.deleteDockerImage(dockerImage);
    }
}
