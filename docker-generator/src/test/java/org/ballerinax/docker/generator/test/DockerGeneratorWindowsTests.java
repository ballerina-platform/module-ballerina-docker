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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.ballerinax.docker.generator.test.utils.DockerTestUtils.removeEnv;
import static org.ballerinax.docker.generator.test.utils.DockerTestUtils.updateEnv;

/**
 * Docker generator tests.
 */
public class DockerGeneratorWindowsTests {

    private static final Path SOURCE_DIR_PATH = Paths.get("src", "test", "resources");
    private final PrintStream out = System.out;

    @Test
    public void buildDockerImageWindowsTest() throws DockerGenException, IOException, ReflectiveOperationException {
        updateEnv("BAL_DOCKER_WINDOWS", "true");
        DockerModel dockerModel = new DockerModel();
        dockerModel.setName("test-image");
        dockerModel.setRegistry("anuruddhal");
        dockerModel.setTag("v1");
        dockerModel.setJarFileName("hello.jar");
        dockerModel.setPorts(Collections.singleton(9090));
        dockerModel.setBuildImage(false);
        dockerModel.setService(true);
        Path jarFilePath = SOURCE_DIR_PATH.resolve("docker-test").resolve("hello.jar");
        Set<Path> jarFilePaths = getJarFilePaths();
        PackageID packageID = new PackageID(new Name("wso2"), new Name("bal"), new Name("1.0.0"));
        dockerModel.setPkgId(packageID);
        dockerModel.setDependencyJarPaths(jarFilePaths);
        CopyFileModel configFile = new CopyFileModel();
        configFile.setSource(SOURCE_DIR_PATH.resolve("conf").resolve("Config.toml").toString());
        configFile.setTarget("/home/ballerina/conf/");
        configFile.setBallerinaConf(true);
        CopyFileModel dataFile = new CopyFileModel();
        dataFile.setSource(SOURCE_DIR_PATH.resolve("conf").resolve("data.txt").toString());
        dataFile.setTarget("/home/ballerina/data/");
        dataFile.setBallerinaConf(true);
        Set<CopyFileModel> externalFiles = new HashSet<>();
        externalFiles.add(configFile);
        externalFiles.add(dataFile);
        dockerModel.setCopyFiles(externalFiles);
        Path outputDir = SOURCE_DIR_PATH.resolve("target");
        Files.createDirectories(outputDir);
        DockerArtifactHandler handler = new DockerArtifactHandler(dockerModel);
        handler.createArtifacts(out, "\t@kubernetes:Docker \t\t\t", jarFilePath, outputDir);
        removeEnv("BAL_DOCKER_WINDOWS");
    }

    @Test(dependsOnMethods = {"buildDockerImageWindowsTest"})
    public void validateDockerFile() throws IOException {
        File dockerFile = SOURCE_DIR_PATH.resolve("target").resolve("Dockerfile").toFile();
        Assert.assertTrue(dockerFile.exists());
        String dockerFileContent = new String(Files.readAllBytes(dockerFile.toPath()));
        Assert.assertTrue(dockerFileContent.contains("CMD java -Xdiag -cp \"hello.jar:jars/*\" " +
                "'wso2/bal/1/$_init'"));
        Assert.assertTrue(dockerFileContent.contains("FROM openjdk:11-windowsservercore"));
    }

    private Set<Path> getJarFilePaths() throws IOException {
        return Files.list(SOURCE_DIR_PATH.resolve("docker-test")).collect(Collectors.toSet());
    }

    @AfterClass
    public void cleanUp() throws IOException, ReflectiveOperationException {
        FileUtils.deleteDirectory(SOURCE_DIR_PATH.resolve("target").toFile());
    }

}
