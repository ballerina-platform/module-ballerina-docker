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

package org.ballerinax.docker;

import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.handlers.DockerArtifactHandler;
import org.ballerinax.docker.models.DockerModel;
import org.ballerinax.docker.utils.DockerGenUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.ballerinax.docker.DockerGenConstants.BALX;
import static org.ballerinax.docker.DockerGenConstants.REGISTRY_SEPARATOR;
import static org.ballerinax.docker.DockerGenConstants.TAG_SEPARATOR;
import static org.ballerinax.docker.utils.DockerGenUtils.printDebug;

/**
 * Process Docker Annotations.
 */
class DockerAnnotationProcessor {

    static DockerModel dockerModel = new DockerModel();
    private PrintStream out = System.out;

    /**
     * Process docker annotations for ballerina Service.
     *
     * @param balxFilePath ballerina file name
     * @param outputDir    target output directory
     */
    void processDockerModel(String balxFilePath, String outputDir) throws DockerPluginException {
        // set docker image name
        if (dockerModel.getName() == null) {
            String defaultImageName = DockerGenUtils.extractBalxName(balxFilePath);
            dockerModel.setName(defaultImageName);
        }
        String registry = dockerModel.getRegistry();
        String imageName = dockerModel.getName();
        imageName = (registry != null) ? registry + REGISTRY_SEPARATOR + imageName + TAG_SEPARATOR
                + dockerModel.getTag() : imageName + TAG_SEPARATOR + dockerModel.getTag();
        dockerModel.setName(imageName);
        dockerModel.setBalxFileName(DockerGenUtils.extractBalxName(balxFilePath) + BALX);

        //TODO: Fix ports with endpoints.
        List<Integer> ports = dockerModel.getPorts();
        if (ports.size() == 0) {
            ports.add(9090);
        }

        if (dockerModel.isEnableDebug()) {
            ports.add(dockerModel.getDebugPort());
        }
        dockerModel.setPorts(ports);
        printDebug(dockerModel.toString());
        createDockerArtifacts(dockerModel, balxFilePath, outputDir);
        printDockerInstructions(dockerModel);
    }


    private void createDockerArtifacts(DockerModel dockerModel, String balxFilePath, String outputDir) throws
            DockerPluginException {
        DockerArtifactHandler dockerArtifactHandler = new DockerArtifactHandler(dockerModel);
        out.print("@docker \t\t - complete 1/3 \r");
        String dockerContent = dockerArtifactHandler.generate();
        try {
            writeToFile(dockerContent, outputDir + File.separator + "Dockerfile");
            String balxDestination = outputDir + File.separator + DockerGenUtils.extractBalxName
                    (balxFilePath) + BALX;
            copyFile(balxFilePath, balxDestination);
            //check image build is enabled.
            if (dockerModel.isBuildImage()) {
                out.print("@docker \t\t - complete 1/3 \r");
                dockerArtifactHandler.buildImage(dockerModel, outputDir);
                Files.delete(Paths.get(balxDestination));
                out.print("@docker \t\t - complete 2/3 \r");
                //push only if image build is enabled.
                if (dockerModel.isPush()) {
                    dockerArtifactHandler.pushImage(dockerModel);
                    out.print("@docker \t\t - complete 3/3 \r");
                }
                out.print("@docker \t\t - complete 3/3 \r");
            }
        } catch (IOException e) {
            throw new DockerPluginException("Unable to write Dockerfile content to " + outputDir);
        } catch (InterruptedException e) {
            throw new DockerPluginException("Unable to create docker images " + e.getMessage());
        }
    }


    private void printDockerInstructions(DockerModel dockerModel) {
        String ansiReset = "\u001B[0m";
        String ansiCyan = "\u001B[36m";
        out.println(ansiCyan + "\nRun following command to start docker container:" + ansiReset);
        StringBuilder command = new StringBuilder("docker run -d ");
        dockerModel.getPorts().forEach((Integer port) -> command.append("-p ").append(port).append(":").append(port)
                .append(" "));
        command.append(dockerModel.getName());
        out.println(ansiCyan + command.toString() + ansiReset);
    }

    /**
     * Copy file from source to destination.
     *
     * @param source      source file path
     * @param destination destination file path
     * @throws DockerPluginException if an error occurs while copying file
     */
    private void copyFile(String source, String destination) throws DockerPluginException {
        File sourceFile = new File(source);
        File destinationFile = new File(destination);
        try (FileInputStream fileInputStream = new FileInputStream(sourceFile);
             FileOutputStream fileOutputStream = new FileOutputStream(destinationFile)) {
            int bufferSize;
            byte[] buffer = new byte[512];
            while ((bufferSize = fileInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bufferSize);
            }
        } catch (IOException e) {
            throw new DockerPluginException("Error while copying file. File not found " + e.getMessage());
        }
    }

    /**
     * Write content to a File. Create the required directories if they don't not exists.
     *
     * @param context        context of the file
     * @param targetFilePath target file path
     * @throws IOException If an error occurs when writing to a file
     */
    private void writeToFile(String context, String targetFilePath) throws IOException {
        File newFile = new File(targetFilePath);
        if (newFile.exists() && newFile.delete()) {
            Files.write(Paths.get(targetFilePath), context.getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (newFile.getParentFile().mkdirs()) {
            Files.write(Paths.get(targetFilePath), context.getBytes(StandardCharsets.UTF_8));
            return;
        }
        Files.write(Paths.get(targetFilePath), context.getBytes(StandardCharsets.UTF_8));
    }
}
