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

package org.ballerinax.docker.test.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.docker.api.model.HostConfig;
import io.fabric8.docker.api.model.ImageInspect;
import io.fabric8.docker.api.model.PortBinding;
import io.fabric8.docker.client.Config;
import io.fabric8.docker.client.ConfigBuilder;
import io.fabric8.docker.client.DockerClient;
import io.fabric8.docker.dsl.EventListener;
import io.fabric8.docker.dsl.OutputHandle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.ballerinax.docker.generator.DockerGenConstants.UNIX_DEFAULT_DOCKER_HOST;
import static org.ballerinax.docker.generator.DockerGenConstants.WINDOWS_DEFAULT_DOCKER_HOST;

/**
 * Docker test utils.
 */
public class DockerTestUtils {

    private static final Log log = LogFactory.getLog(DockerTestUtils.class);
    private static final String DISTRIBUTION_PATH = System.getProperty("ballerina.pack");
    private static final String BALLERINA_COMMAND = DISTRIBUTION_PATH + File.separator + "ballerina";
    private static final String BUILD = "build";
    private static final String RUN = "run";
    private static final String EXECUTING_COMMAND = "Executing command: ";
    private static final String COMPILING = "Compiling: ";
    private static final String RUNNING = "Running: ";
    private static final String EXIT_CODE = "Exit code: ";

    private static String logOutput(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            br.lines().forEach(line -> {
                output.append(line);
                log.info(line);
            });
        }
        return output.toString();
    }

    /**
     * Return a ImageInspect object for a given Docker Image name
     *
     * @param imageName Docker image Name
     * @return ImageInspect object
     */
    public static ImageInspect getDockerImage(String imageName) {
        DockerClient client = getDockerClient();
        return client.image().withName(imageName).inspect();
    }

    /**
     * Delete a given Docker image and prune
     *
     * @param imageName Docker image Name
     */
    public static void deleteDockerImage(String imageName) {
        DockerClient client = getDockerClient();
        client.image().withName(imageName).delete().andPrune();
    }

    public static DockerClient getDockerClient() {
        disableFailOnUnknownProperties();
        String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.getDefault());
        String dockerHost = operatingSystem.contains("win") ? WINDOWS_DEFAULT_DOCKER_HOST : UNIX_DEFAULT_DOCKER_HOST;
        Config dockerClientConfig = new ConfigBuilder()
                .withDockerUrl(dockerHost)
                .build();
        return new io.fabric8.docker.client.DefaultDockerClient(dockerClientConfig);
    }

    /**
     * Compile a ballerina file in a given directory
     *
     * @param sourceDirectory Ballerina source directory
     * @param fileName        Ballerina source file name
     * @return Exit code
     * @throws InterruptedException if an error occurs while compiling
     * @throws IOException          if an error occurs while writing file
     */
    public static int compileBallerinaFile(String sourceDirectory, String fileName) throws InterruptedException,
            IOException {
        ProcessBuilder pb = new ProcessBuilder(BALLERINA_COMMAND, BUILD, fileName);
        log.info(COMPILING + sourceDirectory);
        log.debug(EXECUTING_COMMAND + pb.command());
        pb.directory(new File(sourceDirectory));
        Process process = pb.start();
        int exitCode = process.waitFor();
        log.info(EXIT_CODE + exitCode);
        logOutput(process.getInputStream());
        logOutput(process.getErrorStream());
        return exitCode;
    }

    /**
     * Compile a ballerina project in a given directory
     *
     * @param sourceDirectory Ballerina source directory
     * @return Exit code
     * @throws InterruptedException if an error occurs while compiling
     * @throws IOException          if an error occurs while writing file
     */
    public static int compileBallerinaProject(String sourceDirectory) throws InterruptedException,
            IOException {
        ProcessBuilder pb = new ProcessBuilder(BALLERINA_COMMAND, "init");
        log.info(COMPILING + sourceDirectory);
        log.debug(EXECUTING_COMMAND + pb.command());
        pb.directory(new File(sourceDirectory));
        Process process = pb.start();
        int exitCode = process.waitFor();
        log.info(EXIT_CODE + exitCode);
        logOutput(process.getInputStream());
        logOutput(process.getErrorStream());

        pb = new ProcessBuilder
                (BALLERINA_COMMAND, BUILD);
        log.debug(EXECUTING_COMMAND + pb.command());
        pb.directory(new File(sourceDirectory));
        process = pb.start();
        exitCode = process.waitFor();
        log.info(EXIT_CODE + exitCode);
        logOutput(process.getInputStream());
        logOutput(process.getErrorStream());
        return exitCode;
    }
    
    /**
     * Run a ballerina file in a given directory
     *
     * @param sourceDirectory Ballerina source directory
     * @param fileName        Ballerina source file name
     * @return Exit code
     * @throws InterruptedException if an error occurs while compiling
     * @throws IOException          if an error occurs while writing file
     */
    public static ProcessOutput runBallerinaFile(String sourceDirectory, String fileName) throws InterruptedException,
            IOException {
        ProcessBuilder pb = new ProcessBuilder(BALLERINA_COMMAND, RUN, fileName);
        log.info(RUNNING + sourceDirectory + File.separator + fileName);
        log.debug(EXECUTING_COMMAND + pb.command());
        pb.directory(new File(sourceDirectory));
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        ProcessOutput po = new ProcessOutput();
        log.info(EXIT_CODE + exitCode);
        po.setExitCode(exitCode);
        po.setStdOutput(logOutput(process.getInputStream()));
        po.setErrOutput(logOutput(process.getErrorStream()));
        return po;
    }

    // Disable fail on unknown properties using reflection to avoid docker client issue.
    // (https://github.com/fabric8io/docker-client/issues/106).
    private static void disableFailOnUnknownProperties() {
        try {
            final Field jsonMapperField = Config.class.getDeclaredField("JSON_MAPPER");
            assert jsonMapperField != null;
            jsonMapperField.setAccessible(true);
            final ObjectMapper objectMapper = (ObjectMapper) jsonMapperField.get(null);
            assert objectMapper != null;
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }
    
    /**
     * Create port mapping from host to docker instance.
     *
     * @param hostPort Port of host docker instance.
     * @param containerPort Port of the container.
     * @return The configuration.
     */
    public static HostConfig getPortMappingForHost(Integer hostPort, Integer containerPort) {
        Map<String, ArrayList<PortBinding>> portBinding = new HashMap<>();
        ArrayList<PortBinding> hostPortList = new ArrayList<>();
        PortBinding svcPortBinding = new PortBinding("localhost", hostPort.toString());
        hostPortList.add(svcPortBinding);
        portBinding.put(containerPort.toString() + "/tcp", hostPortList);
    
        HostConfig hostConfig = new HostConfig();
        hostConfig.setPortBindings(portBinding);
        return hostConfig;
    }
    
    /**
     * Start a docker container and wait until a ballerina service starts.
     *
     * @param containerID ID of the container.
     * @return true if service started, else false.
     * @throws IOException          Error when closing log reader.
     * @throws InterruptedException Error when waiting for service start.
     */
    public static boolean startService(String containerID) throws IOException, InterruptedException {
        DockerError error = new DockerError();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final PrintStream out = System.out;
        getDockerClient().container().withName(containerID).start();
        OutputHandle handle =
                getDockerClient().container().withName(containerID).logs().writingOutput(out).writingError(out).
                usingListener(new EventListener() {
                    @Override
                    public void onSuccess(String message) {
                    }
                    
                    @Override
                    public void onError(String message) {
                        error.setErrorMsg(message);
                        countDownLatch.countDown();
                    }
                    
                    @Override
                    public void onError(Throwable t) {
                        if (!(t instanceof SocketTimeoutException)) {
                            t.printStackTrace(System.out);
                            error.setErrorMsg(t.getMessage());
                        }
                        countDownLatch.countDown();
                    }
                    
                    @Override
                    public void onEvent(String event) {
                        if (event.contains("[ballerina/http] started HTTP/WS endpoint")) {
                            countDownLatch.countDown();
                        }
                    }
                }).follow();
        
        countDownLatch.await();
        handle.close();
        if (error.isError()) {
            log.error(error.getErrorMsg());
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * Create a container.
     *
     * @param dockerImage   Docker image name.
     * @param containerName The name of the container.
     * @return The container ID.
     */
    public static String createContainer(String dockerImage, String containerName) {
        return getDockerClient().container()
                .createNew()
                .withName(containerName)
                .withHostConfig(DockerTestUtils.getPortMappingForHost(9090, 9090))
                .withImage(dockerImage)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .done().getId();
    }
    
    /**
     * Class to hold docker errors.
     */
    private static class DockerError {
        private boolean error;
        private String errorMsg;
    
        DockerError() {
            this.error = false;
        }
    
        boolean isError() {
            return error;
        }
    
        String getErrorMsg() {
            return errorMsg;
        }
    
        void setErrorMsg(String errorMsg) {
            this.error = true;
            this.errorMsg = errorMsg;
        }
    }
}
