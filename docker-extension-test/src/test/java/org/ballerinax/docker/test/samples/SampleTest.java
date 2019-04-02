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

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.test.utils.DockerTestException;
import org.ballerinax.docker.test.utils.DockerTestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.ballerinax.docker.generator.DockerGenConstants.BALLERINA_BASE_IMAGE;

/**
 * Base class for test cases written to test samples.
 */
public abstract class SampleTest {
    
    private static final Log log = LogFactory.getLog(SampleTest.class);
    private static final Path DOCKER_FILE = Paths.get(FilenameUtils.separatorsToSystem(
            System.getProperty("dockerfile"))).toAbsolutePath().normalize();
    private static final Path BALLERINA_RUNTIME_DIR = Paths.get(FilenameUtils.separatorsToSystem(
            System.getProperty("ballerina.pack"))).toAbsolutePath();
    private static final Path TEMP_DIR = BALLERINA_RUNTIME_DIR.getParent().resolve("tmp");
    private static final Path DOCKER_FILE_COPY = BALLERINA_RUNTIME_DIR.getParent().resolve("Dockerfile");
    private static final Path BALLERINA_RUNTIME_ZIP = Paths.get(BALLERINA_RUNTIME_DIR + ".zip");
    private String dockerImage = BALLERINA_BASE_IMAGE + ":" + System.getProperty("docker.image.version");
    protected static final Path SAMPLE_DIR = Paths.get(FilenameUtils.separatorsToSystem(
            System.getProperty("sample.dir")));
    protected static final Path CLIENT_BAL_FOLDER = Paths.get("src").resolve("test").resolve("resources")
            .resolve("sample-clients")
            .toAbsolutePath();
    String builtImageID = null;
    
    @BeforeSuite
    public void buildDockerImage() throws IOException, DockerTestException, DockerException, InterruptedException {
        // make temporary folder.
        FileUtils.forceMkdir(TEMP_DIR.toFile());
        
        // copy pack into temporary folder.
        FileUtils.copyDirectory(BALLERINA_RUNTIME_DIR.toFile(),
                TEMP_DIR.resolve(BALLERINA_RUNTIME_DIR.getFileName()).toFile(), true);
        
        // compress the copied ballerina distribution.
        compressFiles(TEMP_DIR, new FileOutputStream(BALLERINA_RUNTIME_ZIP.toFile()));
        
        // delete tmp directory
        FileUtils.deleteQuietly(TEMP_DIR.toFile());
        
        // copy extracted ballerina distribution to the /docker/base directory.
        FileUtils.copyFile(DOCKER_FILE.toFile(), DOCKER_FILE_COPY.toFile());
        
        // Passing build argument.
        String ballerinaDistBuildArg = "{\"BALLERINA_DIST\":\"" +
                                       FilenameUtils.getName(BALLERINA_RUNTIME_ZIP.toString()) + "\"}";
        
        CountDownLatch buildDone = new CountDownLatch(1);
        final AtomicReference<String> errorAtomicReference = new AtomicReference<>();
        builtImageID = DockerTestUtils.getDockerClient().build(DOCKER_FILE_COPY.getParent(), dockerImage, message -> {
            String buildImageId = message.buildImageId();
            String error = message.error();
            String stream = message.stream();
            
            if (stream != null) {
                log.info(stream.replaceAll("\\n", ". "));
            }
            
            // when an image is built successfully.
            if (null != buildImageId) {
                buildDone.countDown();
            }
            
            if (error != null) {
                errorAtomicReference.set(error);
                buildDone.countDown();
            }
        }, DockerClient.BuildParam.noCache(),
                DockerClient.BuildParam.forceRm(),
                DockerClient.BuildParam.create("buildargs", URLEncoder.encode(ballerinaDistBuildArg,
                        Charsets.UTF_8.displayName())));
        
        buildDone.await();
        String dockerErrorMsg = errorAtomicReference.get();
        if (null != dockerErrorMsg) {
            log.error(dockerErrorMsg);
            Assert.fail();
        }
        
        log.info("Ballerina base image built: " + builtImageID);
        Assert.assertNotNull(DockerTestUtils.getDockerClient().inspectImage(dockerImage));
    }
    
    @BeforeClass
    abstract void compileSample() throws IOException, InterruptedException;
    
    @AfterClass
    abstract void cleanUp() throws DockerPluginException, InterruptedException, DockerTestException;
    
    @AfterSuite
    public void deleteDockerImage() throws DockerTestException {
        if (null != builtImageID) {
            log.info("Removing built ballerina base image:" + builtImageID);
//            DockerTestUtils.deleteDockerImage(dockerImage);
        }
    }
    
    /**
     * Compresses files.
     *
     * @param outputStream outputstream
     * @return outputstream of the compressed file
     * @throws IOException exception if an error occurrs when compressing
     */
    private static void compressFiles(Path dir, OutputStream outputStream) throws IOException {
        dir = dir.resolve("");
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        if (Files.isRegularFile(dir)) {
            Path fileName = dir.getFileName();
            if (fileName != null) {
                addEntry(zos, dir, fileName.toString());
            } else {
                Assert.fail("Error occurred when compressing");
            }
        } else {
            Stream<Path> list = Files.walk(dir);
            Path finalDir = dir;
            list.forEach(p -> {
                StringJoiner joiner = new StringJoiner("/");
                for (Path path : finalDir.relativize(p)) {
                    joiner.add(path.toString());
                }
                if (Files.isRegularFile(p)) {
                    try {
                        addEntry(zos, p, joiner.toString());
                    } catch (IOException e) {
                        Assert.fail("Error occurred when compressing");
                    }
                }
            });
        }
        zos.close();
    }
    
    
    /**
     * Add file inside the src directory to the ZipOutputStream.
     *
     * @param zos      ZipOutputStream
     * @param filePath file path of each file inside the driectory
     * @throws IOException exception if an error occurrs when compressing
     */
    private static void addEntry(ZipOutputStream zos, Path filePath, String fileStr) throws IOException {
        ZipEntry ze = new ZipEntry(fileStr);
        zos.putNextEntry(ze);
        Files.copy(filePath, zos);
        zos.closeEntry();
    }

}
