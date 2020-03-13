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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Docker test utils.
 */
public class DockerTestUtils {

    private static final Log log = LogFactory.getLog(DockerTestUtils.class);
    private static final String JAVA_OPTS = "JAVA_OPTS";
    private static final String DISTRIBUTION_PATH = FilenameUtils.separatorsToSystem(
            System.getProperty("ballerina.pack"));
    private static final String BALLERINA_COMMAND = DISTRIBUTION_PATH +
            File.separator + "bin" +
            File.separator + (System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("win") ?
            "ballerina.bat" : "ballerina");
    private static final String BUILD = "build";
    private static final String RUN = "run";
    private static final String EXECUTING_COMMAND = "Executing command: ";
    private static final String COMPILING = "Compiling: ";
    private static final String RUNNING = "Running: ";
    private static final String EXIT_CODE = "Exit code: ";
    private static final Integer LOG_WAIT_COUNT = 10;
    private static String serviceIP = "localhost";

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

    public static DockerClient getDockerClient() {
        DefaultDockerClientConfig.Builder dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder();
        // if windows, consider DOCKER_HOST as "tcp://localhost:2375"
        if (System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("win")) {
            dockerClientConfig.withDockerHost("tcp://localhost:2375");
        }
        return DockerClientBuilder.getInstance(dockerClientConfig.build()).build();
    }

    /**
     * Return a ImageInspect object for a given Docker Image name.
     *
     * @param imageName Docker image Name
     * @return ImageInspect object
     */
    public static InspectImageResponse getDockerImage(String imageName) {
        return getDockerClient().inspectImageCmd(imageName).exec();
    }

    /**
     * Get the list of exposed ports of the docker image.
     *
     * @param imageName The docker image name.
     * @return Exposed ports.
     */
    public static List<String> getExposedPorts(String imageName) {
        InspectImageResponse dockerImage = getDockerImage(imageName);
        if (null == dockerImage.getConfig() || null == dockerImage.getConfig().getExposedPorts()) {
            return new ArrayList<>();
        }

        ExposedPort[] exposedPorts = dockerImage.getConfig().getExposedPorts();
        return Arrays.stream(exposedPorts).map(ExposedPort::toString).collect(Collectors.toList());
    }

    /**
     * Get the list of commands of the docker image.
     *
     * @param imageName The docker image name.
     * @return The list of commands.
     */
    public static List<String> getCommand(String imageName) {
        InspectImageResponse dockerImage = getDockerImage(imageName);
        if (null == dockerImage.getConfig() || null == dockerImage.getConfig().getCmd()) {
            return new ArrayList<>();
        }

        return Arrays.asList(dockerImage.getConfig().getCmd());
    }

    /**
     * Delete a given Docker image and prune.
     *
     * @param imageName Docker image Name
     */
    public static void deleteDockerImage(String imageName) {
        getDockerClient().removeImageCmd(imageName).withForce(true).withNoPrune(false).exec();
    }

    /**
     * Compile a ballerina file in a given directory.
     *
     * @param sourceDirectory Ballerina source directory
     * @param fileName        Ballerina source file name
     * @return Exit code
     * @throws InterruptedException if an error occurs while compiling
     * @throws IOException          if an error occurs while writing file
     */
    public static ProcessOutput compileBallerinaFile(Path sourceDirectory, String fileName) throws InterruptedException,
            IOException {
        return compileBallerinaFile(sourceDirectory, fileName, null);
    }


    /**
     * Compile a ballerina file in a given directory.
     *
     * @param sourceDirectory Ballerina source directory
     * @param fileName        Ballerina source file name
     * @return Exit code
     * @throws InterruptedException if an error occurs while compiling
     * @throws IOException          if an error occurs while writing file
     */
    public static ProcessOutput compileBallerinaFile(Path sourceDirectory, String fileName, Map<String, String> envVars)
            throws InterruptedException, IOException {

        Path ballerinaInternalLog = Paths.get(sourceDirectory.toAbsolutePath().toString(), "ballerina-internal.log");
        if (ballerinaInternalLog.toFile().exists()) {
            log.warn("Deleting already existing ballerina-internal.log file.");
            FileUtils.deleteQuietly(ballerinaInternalLog.toFile());
        }

        ProcessBuilder pb = new ProcessBuilder(BALLERINA_COMMAND, BUILD, fileName);
        log.info(COMPILING + sourceDirectory.normalize().resolve(fileName));
        log.debug(EXECUTING_COMMAND + pb.command());
        pb.directory(sourceDirectory.toFile());
        Map<String, String> environment = pb.environment();
        addJavaAgents(environment);
        if (envVars != null) {
            envVars.forEach(environment::put);
        }
        Process process = pb.start();
        int exitCode = process.waitFor();

        // log ballerina-internal.log content
        if (Files.exists(ballerinaInternalLog)) {
            log.info("ballerina-internal.log file found. content: ");
            log.info(FileUtils.readFileToString(ballerinaInternalLog.toFile(), Charset.defaultCharset()));
        }

        ProcessOutput po = new ProcessOutput();
        log.info(EXIT_CODE + exitCode);
        po.setExitCode(exitCode);
        po.setStdOutput(logOutput(process.getInputStream()));
        po.setErrOutput(logOutput(process.getErrorStream()));
        return po;
    }

    /**
     * Compile a ballerina project module in a given directory.
     *
     * @param sourceDirectory Ballerina source directory
     * @param moduleName      Module name.
     * @return Exit code
     * @throws InterruptedException if an error occurs while compiling
     * @throws IOException          if an error occurs while writing file
     */
    public static int compileBallerinaProjectModule(Path sourceDirectory, String moduleName) throws
            InterruptedException, IOException {
        Path ballerinaInternalLog = Paths.get(sourceDirectory.toAbsolutePath().toString(), "ballerina-internal.log");
        if (ballerinaInternalLog.toFile().exists()) {
            log.warn("Deleting already existing ballerina-internal.log file.");
            FileUtils.deleteQuietly(ballerinaInternalLog.toFile());
        }

        ProcessBuilder pb = new ProcessBuilder(BALLERINA_COMMAND, BUILD, moduleName);
        log.info(COMPILING + sourceDirectory.normalize());
        log.debug(EXECUTING_COMMAND + pb.command());
        pb.directory(sourceDirectory.toFile());
        Map<String, String> environment = pb.environment();
        addJavaAgents(environment);

        Process process = pb.start();
        int exitCode = process.waitFor();
        log.info(EXIT_CODE + exitCode);
        logOutput(process.getInputStream());
        logOutput(process.getErrorStream());

        // log ballerina-internal.log content
        if (Files.exists(ballerinaInternalLog)) {
            log.info("ballerina-internal.log file found. content: ");
            log.info(FileUtils.readFileToString(ballerinaInternalLog.toFile(), Charset.defaultCharset()));
        }

        return exitCode;
    }

    /**
     * Compile a ballerina project in a given directory.
     *
     * @param sourceDirectory Ballerina source directory
     * @return Exit code
     * @throws InterruptedException if an error occurs while compiling
     * @throws IOException          if an error occurs while writing file
     */
    public static int compileBallerinaProject(Path sourceDirectory) throws InterruptedException,
            IOException {
        Path ballerinaInternalLog = Paths.get(sourceDirectory.toAbsolutePath().toString(), "ballerina-internal.log");
        if (ballerinaInternalLog.toFile().exists()) {
            log.warn("Deleting already existing ballerina-internal.log file.");
            FileUtils.deleteQuietly(ballerinaInternalLog.toFile());
        }

        ProcessBuilder pb = new ProcessBuilder(BALLERINA_COMMAND, BUILD, "-a");
        log.info(COMPILING + sourceDirectory.normalize());
        log.debug(EXECUTING_COMMAND + pb.command());
        pb.directory(sourceDirectory.toFile());
        Map<String, String> environment = pb.environment();
        addJavaAgents(environment);

        Process process = pb.start();
        int exitCode = process.waitFor();
        log.info(EXIT_CODE + exitCode);
        logOutput(process.getInputStream());
        logOutput(process.getErrorStream());

        // log ballerina-internal.log content
        if (Files.exists(ballerinaInternalLog)) {
            log.info("ballerina-internal.log file found. content: ");
            log.info(FileUtils.readFileToString(ballerinaInternalLog.toFile(), Charset.defaultCharset()));
        }

        return exitCode;
    }

    /**
     * Run a ballerina file in a given directory.
     *
     * @param sourceDirectory Ballerina source directory
     * @param fileName        Ballerina source file name
     * @return Exit code
     * @throws InterruptedException if an error occurs while compiling
     * @throws IOException          if an error occurs while writing file
     */
    public static ProcessOutput runBallerinaFile(Path sourceDirectory, String fileName)
            throws InterruptedException, IOException {

        ProcessBuilder pb = new ProcessBuilder(BALLERINA_COMMAND, RUN, fileName, serviceIP);
        log.info(RUNNING + sourceDirectory.resolve(fileName));
        log.debug(EXECUTING_COMMAND + pb.command());
        pb.directory(sourceDirectory.toFile());
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
     * @param dockerPortBindings Ports needed exposed. Key is docker instance port and value is host port.
     * @return The configuration.
     */
    private static HostConfig getPortMappingForHost(List<Integer> dockerPortBindings) {
        List<PortBinding> portBindings = new ArrayList<>();

        for (Integer dockerPortBinding : dockerPortBindings) {
            PortBinding portBinding = new PortBinding(
                    Ports.Binding.bindIpAndPort("0.0.0.0", Integer.parseInt(dockerPortBinding.toString())),
                    ExposedPort.parse(dockerPortBinding.toString()));
            portBindings.add(portBinding);
        }

        return HostConfig.newHostConfig().withPortBindings(portBindings);
    }

    /**
     * Create a container with host and container ports.
     *
     * @param dockerImage   Docker image name.
     * @param containerName The name of the container.
     * @param portBindings  Ports to be exposed. Key is docker instance port and value is host port.
     * @return The container ID.
     */
    public static String createContainer(String dockerImage, String containerName, List<Integer> portBindings) {
        return getDockerClient().createContainerCmd(dockerImage)
                .withHostConfig(getPortMappingForHost(portBindings))
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withName(containerName)
                .exec()
                .getId();
    }

    /**
     * Create a container. Created port binding of 9090 to 9090 between host and port.
     *
     * @param dockerImage   Docker image name.
     * @param containerName The name of the container.
     * @return The container ID.
     */
    public static String createContainer(String dockerImage, String containerName) {
        return createContainer(dockerImage, containerName, Collections.singletonList(9090));
    }

    /**
     * Start a docker container and wait until a ballerina service starts.
     *
     * @param containerID ID of the container.
     * @param logToWait   Log message to confirm waiting
     * @return true if service started, else false.
     */
    public static boolean startContainer(String containerID, String logToWait) throws DockerTestException {
        try {
            DockerClient dockerClient = getDockerClient();
            log.info("Starting container: " + containerID);

            dockerClient.startContainerCmd(containerID).exec();

            int logWaitCount = 0;
            boolean containerStarted = false;
            StringBuilder containerLogs = new StringBuilder();

            while (logWaitCount < LOG_WAIT_COUNT) {
                log.info("Waiting for container startup " + (logWaitCount + 1) + "/" + LOG_WAIT_COUNT);
                dockerClient.logContainerCmd(containerID)
                        .withStdErr(true)
                        .withStdOut(true)
                        .withFollowStream(true)
                        .withTailAll()
                        .exec(new LogContainerResultCallback() {
                            @Override
                            public void onNext(Frame item) {
                                containerLogs.append((new String(item.getPayload())).trim()).append("\n");
                                super.onNext(item);
                            }
                        }).awaitCompletion(3, TimeUnit.SECONDS);

                if (containerLogs.toString().trim().contains(logToWait)) {
                    containerStarted = true;
                    break;
                }
                logWaitCount++;
            }

            if (containerStarted) {
                log.info("Container started: " + containerID);

                // Find docker container IP address if such exists
                InspectContainerResponse containerInfo = getDockerClient().inspectContainerCmd(containerID).exec();

                String os = System.getProperty("os.name").toLowerCase(Locale.getDefault());
                // If OS is linux
                if ((os.contains("nix") || os.contains("nux") || os.contains("aix")) &&
                        !"".equals(containerInfo.getNetworkSettings().getIpAddress())) {
                    serviceIP = containerInfo.getNetworkSettings().getIpAddress();
                }

                log.info("Container IP address found as: " + serviceIP);

                return true;
            } else {
                log.error("Container did not start: " + containerLogs);
                return false;
            }
        } catch (InterruptedException ex) {
            throw new DockerTestException(ex);
        }
    }

    /**
     * Stop and remove a running container.
     *
     * @param containerID The container ID.
     */
    public static void stopContainer(String containerID) {
        try {
            InspectContainerResponse containerInfo = getDockerClient().inspectContainerCmd(containerID).exec();
            if ((null != containerInfo.getState().getRestarting() && containerInfo.getState().getRestarting()) ||
                    (null != containerInfo.getState().getPaused() && containerInfo.getState().getPaused()) ||
                    (null != containerInfo.getState().getRunning() && containerInfo.getState().getRunning())) {
                log.info("Stopping container: " + containerID);
                getDockerClient().stopContainerCmd(containerID).exec();
            }
            log.info("Removing container: " + containerID);
            getDockerClient().removeContainerCmd(containerID).exec();
        } catch (NotFoundException | NotModifiedException e) {
            // ignore
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
