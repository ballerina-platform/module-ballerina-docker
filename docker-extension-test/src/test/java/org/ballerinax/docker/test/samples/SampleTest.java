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
import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.generator.DockerGenConstants;
import org.ballerinax.docker.test.utils.DockerTestException;
import org.ballerinax.docker.test.utils.DockerTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.ballerinax.docker.generator.DockerGenConstants.BALLERINA_BASE_IMAGE;
import static org.ballerinax.docker.generator.DockerGenConstants.TAG_SEPARATOR;

/**
 * Base class for test cases written to test samples.
 */
public abstract class SampleTest {
    
    private final Logger log = LoggerFactory.getLogger(SampleTest.class);

    private static final boolean WINDOWS_BUILD = "true".equals(System.getenv(DockerGenConstants.ENABLE_WINDOWS_BUILD));

    /**
     * Location of the ballerina docker base image.
     */
    private static final Path DOCKER_FILE = WINDOWS_BUILD ?
            Paths.get(System.getProperty("dockerfile.windows")).toAbsolutePath().normalize() :
            Paths.get(System.getProperty("dockerfile")).toAbsolutePath().normalize();
    
    /**
     * Location of the extracted ballerina pack.
     */
    private static final Path BALLERINA_RUNTIME_DIR = Paths.get(FilenameUtils.separatorsToSystem(
            System.getProperty("ballerina.pack"))).toAbsolutePath();
    
    /**
     * Location where the base image is copied in order to build the image.
     */
    private static final Path DOCKER_FILE_COPY = BALLERINA_RUNTIME_DIR.getParent().resolve("Dockerfile");
    
    /**
     * The name of the ballerina zip file.
     */
    private static final String BALLERINA_RUNTIME_ZIP_NAME = BALLERINA_RUNTIME_DIR.getFileName() + ".zip";
    
    /**
     * The docker base image name.
     */
    private static final String DOCKER_IMAGE = BALLERINA_BASE_IMAGE + TAG_SEPARATOR +
                                               System.getProperty("docker.image.version");
    
    /**
     * Location of the samples directory.
     */
    protected static final Path SAMPLE_DIR = Paths.get(FilenameUtils.separatorsToSystem(
            System.getProperty("sample.dir")));
    
    /**
     * Location where clients for the samples are residing in.
     */
    protected static final Path CLIENT_BAL_FOLDER = Paths.get("src").resolve("test").resolve("resources")
            .resolve("sample-clients")
            .toAbsolutePath();
    
    String builtImageID = null;
    
    @BeforeSuite
    public void buildDockerImage() throws IOException, DockerTestException, DockerException, InterruptedException {
        // copy extracted ballerina distribution to the /docker/base directory.
        FileUtils.copyFile(DOCKER_FILE.toFile(), DOCKER_FILE_COPY.toFile());

        // Passing build argument.
        String ballerinaDistBuildArg = "{\"BALLERINA_DIST\":\"" + BALLERINA_RUNTIME_ZIP_NAME + "\"}";

        CountDownLatch buildDone = new CountDownLatch(1);
        final AtomicReference<String> errorAtomicReference = new AtomicReference<>();
        builtImageID = DockerTestUtils.getDockerClient().build(DOCKER_FILE_COPY.getParent(), DOCKER_IMAGE, message -> {
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
        Assert.assertNotNull(DockerTestUtils.getDockerClient().inspectImage(DOCKER_IMAGE));
    }
    
    @BeforeClass
    abstract void compileSample() throws IOException, InterruptedException;
    
    @AfterClass
    abstract void cleanUp() throws DockerPluginException, InterruptedException, DockerTestException;
    
    @AfterSuite
    public void deleteDockerImage() throws DockerTestException {
        if (null != builtImageID) {
            log.info("Removing built ballerina base image:" + builtImageID);
            DockerTestUtils.deleteDockerImage(DOCKER_IMAGE);
        }
    }
}
