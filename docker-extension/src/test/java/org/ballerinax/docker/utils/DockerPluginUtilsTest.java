/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinax.docker.utils;

import org.apache.commons.io.FileUtils;
import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.generator.utils.DockerGenUtils;
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
import java.util.Map;

/**
 * Docker Utils Test Class.
 */
public class DockerPluginUtilsTest {

    private Path tempDirectory;

    @BeforeClass
    public void setUp() throws IOException {
        tempDirectory = Files.createTempDirectory("ballerinax-docker-plugin-");
    }

    @Test
    public void extractJarNameTest() {
        Path jarFilePath =
                Paths.get("/Users/anuruddha/workspace/ballerinax/docker/samples/sample5/hello_config_file.jar");
        String jarFileName = "hello_config_file";
        Assert.assertEquals(DockerGenUtils.extractJarName(jarFilePath), jarFileName);
        jarFilePath = Paths.get("/Users/anuruddha/workspace/ballerinax/docker/samples/sample5/");
        Assert.assertNull(DockerGenUtils.extractJarName(jarFilePath));
    }

    @Test
    public void isBlankTest() {
        Assert.assertTrue(DockerPluginUtils.isBlank(""));
        Assert.assertTrue(DockerPluginUtils.isBlank(" "));
        Assert.assertTrue(DockerPluginUtils.isBlank(null));
        Assert.assertFalse(DockerPluginUtils.isBlank("value"));
    }

    @Test
    public void resolveValueTest() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("DOCKER_USERNAME", "anuruddhal");
        env.put("DOCKER_PASSWORD", "");
        setEnv(env);
        Assert.assertEquals(DockerPluginUtils.resolveValue("$env{DOCKER_USERNAME}"), "anuruddhal");
        Assert.assertEquals(DockerPluginUtils.resolveValue("$env{DOCKER_PASSWORD}"), "");
        Assert.assertEquals(DockerPluginUtils.resolveValue("$env{DOCKER_UNDEFINED}"), "");
        Assert.assertEquals(DockerPluginUtils.resolveValue("demo"), "demo");
    }

    @Test
    public void deleteDirectoryTest() throws IOException, DockerPluginException {
        File file = tempDirectory.resolve("myfile.txt").toFile();
        Assert.assertTrue(file.createNewFile());
        File directory = tempDirectory.resolve("subFolder").toFile();
        Assert.assertTrue(directory.mkdirs());
        DockerPluginUtils.deleteDirectory(file.toPath());
        Assert.assertFalse(file.exists(), "myfile.txt not deleted");
        DockerPluginUtils.deleteDirectory(directory.toPath());
        Assert.assertFalse(directory.exists(), "subFolder not deleted");
    }

    private void setEnv(Map<String, String> newenv) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField
                    ("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
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
                    map.putAll(newenv);
                }
            }
        }
    }

    @AfterClass
    public void cleanUp() {
        FileUtils.deleteQuietly(tempDirectory.toFile());
    }
}
