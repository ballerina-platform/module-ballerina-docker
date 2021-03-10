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

package org.ballerinax.docker.generator.test;

import org.apache.commons.io.FileUtils;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinax.docker.generator.DockerArtifactHandler;
import org.ballerinax.docker.generator.exceptions.DockerGenException;
import org.ballerinax.docker.generator.models.CopyFileModel;
import org.ballerinax.docker.generator.models.DockerModel;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.util.Name;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Docker Invalid Copy tests.
 */
public class DockerInvalidCopyTest {

    private static final Path SOURCE_DIR_PATH = Paths.get("src", "test", "resources");
    private final PrintStream out = System.out;

    @Test
    public void buildDockerImageTest() throws IOException, DockerGenException {
        DockerModel dockerModel = new DockerModel();
        dockerModel.setName("test-image");
        dockerModel.setRegistry("anuruddhal");
        dockerModel.setTag("v1");
        dockerModel.setJarFileName("hello.jar");
        dockerModel.setPorts(Collections.singleton(9090));
        dockerModel.setBuildImage(true);
        dockerModel.setService(false);
        DockerArtifactHandler handler = new DockerArtifactHandler(dockerModel);
        Path jarFilePath = SOURCE_DIR_PATH.resolve("docker-test").resolve("hello.jar");
        Set<Path> jarFilePaths = getJarFilePaths();
        PackageID packageID = new PackageID(new Name("wso2"), new Name("bal"), new Name("1.0.0"));
        dockerModel.setPkgId(packageID);
        CopyFileModel configFile = new CopyFileModel();
        configFile.setSource(SOURCE_DIR_PATH.resolve("conf1").resolve("Config.toml").toString());
        configFile.setTarget("/home/ballerina/conf/");
        configFile.setBallerinaConf(true);
        dockerModel.setCopyFiles(Collections.singleton(configFile));
        dockerModel.setDependencyJarPaths(jarFilePaths);
        Path outputDir = SOURCE_DIR_PATH.resolve("target");
        Files.createDirectories(outputDir);
        try {
            handler.createArtifacts(out, "\t@kubernetes:Docker \t\t\t", jarFilePath, outputDir);
            Assert.fail("Expected error not thrown");
        } catch (DockerGenException e) {
            Assert.assertTrue(e.getMessage().contains("does not exist"));

        }
    }

    private Set<Path> getJarFilePaths() throws IOException {
        return Files.list(SOURCE_DIR_PATH.resolve("docker-test")).collect(Collectors.toSet());
    }

    @AfterClass
    public void cleanUp() throws IOException {
        FileUtils.deleteDirectory(SOURCE_DIR_PATH.resolve("target").toFile());
    }

}
