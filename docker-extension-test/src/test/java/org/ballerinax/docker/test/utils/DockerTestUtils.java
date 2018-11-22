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

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ImageInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.internal.RuntimeDelegateImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.ws.rs.ext.RuntimeDelegate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.ballerinax.docker.generator.DockerGenConstants.UNIX_DEFAULT_DOCKER_HOST;
import static org.ballerinax.docker.generator.DockerGenConstants.WINDOWS_DEFAULT_DOCKER_HOST;

/**
 * Docker test utils.
 */
public class DockerTestUtils {

    private static final Log log = LogFactory.getLog(DockerTestUtils.class);
    private static final String JAVA_OPTS = "JAVA_OPTS";
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
    public static ImageInfo getDockerImage(String imageName) throws DockerTestException, InterruptedException {
        try {
            DockerClient client = getDockerClient();
            return client.inspectImage(imageName);
        } catch (DockerException e) {
            throw new DockerTestException(e);
        }
    }
    
    /**
     * Get the list of exposed ports of the docker image.
     *
     * @param imageName The docker image name.
     * @return Exposed ports.
     * @throws DockerTestException      If issue occurs inspecting docker image
     * @throws InterruptedException If issue occurs inspecting docker image
     */
    public static List<String> getExposedPorts(String imageName) throws DockerTestException, InterruptedException {
        ImageInfo dockerImage = getDockerImage(imageName);
        return Objects.requireNonNull(dockerImage.config().exposedPorts()).asList();
    }
    
    /**
     * Get the list of commands of the docker image.
     *
     * @param imageName The docker image name.
     * @return The list of commands.
     * @throws DockerTestException      If issue occurs inspecting docker image
     * @throws InterruptedException If issue occurs inspecting docker image
     */
    public static List<String> getCommand(String imageName) throws DockerTestException, InterruptedException {
        ImageInfo dockerImage = getDockerImage(imageName);
        return dockerImage.config().cmd();
    }

    /**
     * Delete a given Docker image and prune
     *
     * @param imageName Docker image Name
     */
    public static void deleteDockerImage(String imageName) throws DockerTestException, InterruptedException {
        try {
            DockerClient client = getDockerClient();
            client.removeImage(imageName, true, false);
        } catch (DockerException e) {
            throw new DockerTestException(e);
        }
    }

    public static DockerClient getDockerClient() {
        RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
        String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.getDefault());
        String dockerHost = operatingSystem.contains("win") ? WINDOWS_DEFAULT_DOCKER_HOST : UNIX_DEFAULT_DOCKER_HOST;
        return DefaultDockerClient.builder().uri(dockerHost).build();
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
        Map<String, String> environment = pb.environment();
        addJavaAgents(environment);
        
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
        Map<String, String> environment = pb.environment();
        addJavaAgents(environment);
    
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
     * Create a container with host and container ports.
     *
     * @param dockerImage   Docker image name.
     * @param containerName The name of the container.
     * @return The container ID.
     */
    public static String createContainer(String dockerImage, String containerName, int hostPort, int containerPort) {
        return getDockerClient().container()
                .createNew()
                .withName(containerName)
                .withHostConfig(DockerTestUtils.getPortMappingForHost(hostPort, containerPort))
                .withImage(dockerImage)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .done().getId();
    }
    
    /**
     * Create a container.
     *
     * @param dockerImage   Docker image name.
     * @param containerName The name of the container.
     * @return The container ID.
     */
    public static String createContainer(String dockerImage, String containerName) {
        return createContainer(dockerImage, containerName, 9090, 9090);
    }
    
    /**
     * Start a docker container and wait until a ballerina service starts.
     *
     * @param containerID ID of the container.
     * @param logToWait   Log message to confirm waiting
     * @return true if service started, else false.
     * @throws IOException          Error when closing log reader.
     * @throws InterruptedException Error when waiting for service start.
     */
    public static boolean startContainer(String containerID, String logToWait) throws IOException,
            InterruptedException {
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
                                // Ignoring logging socket timeout exception due to okio async timeout.
                                if (!(t instanceof SocketTimeoutException) && !(t instanceof InterruptedIOException)) {
                                    t.printStackTrace(System.out);
                                    error.setErrorMsg(t.getMessage());
                                }
                                countDownLatch.countDown();
                            }
                            
                            @Override
                            public void onEvent(String event) {
                                if (event.contains(logToWait)) {
                                    countDownLatch.countDown();
                                }
                            }
                        }).follow();
    
        boolean awaitResult = countDownLatch.await(5, TimeUnit.SECONDS);
        handle.close();
        if (error.isError()) {
            log.error(error.getErrorMsg());
            return false;
        } else {
            return awaitResult;
        }
    }
    
    /**
     * Stop and remove a running container.
     *
     * @param containerID The container ID.
     */
    public static void stopContainer(String containerID) {
        if (null != containerID) {
            // stop container
            getDockerClient().container()
                    .withName(containerID)
                    .stop();
        
            // remove container
            getDockerClient().container()
                    .withName(containerID)
                    .remove();
        }
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
    
    private static synchronized void addJavaAgents(Map<String, String> envProperties) {
        String javaOpts = "";
        if (envProperties.containsKey(JAVA_OPTS)) {
            javaOpts = envProperties.get(JAVA_OPTS);
        }
        if (javaOpts.contains("jacoco.agent")) {
            return;
        }
        javaOpts = getJacocoAgentArgs() + javaOpts;
        envProperties.put(JAVA_OPTS, javaOpts);
    }
    
    private static String getJacocoAgentArgs() {
        String jacocoArgLine = System.getProperty("jacoco.agent.argLine");
        if (jacocoArgLine == null || jacocoArgLine.isEmpty()) {
            log.warn("Running integration test without jacoco test coverage");
            return "";
        }
        return jacocoArgLine + " ";
    }
}
