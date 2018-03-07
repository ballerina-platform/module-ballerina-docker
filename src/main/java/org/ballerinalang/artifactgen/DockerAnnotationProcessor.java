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

package org.ballerinalang.artifactgen;

import org.ballerinalang.artifactgen.handlers.DockerArtifactHandler;
import org.ballerinalang.artifactgen.models.DockerModel;
import org.ballerinalang.artifactgen.utils.DockerGenUtils;
import org.ballerinalang.util.codegen.AnnAttachmentInfo;
import org.ballerinalang.util.codegen.ServiceInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.ballerinalang.artifactgen.utils.DockerGenUtils.extractPorts;
import static org.ballerinalang.artifactgen.utils.DockerGenUtils.printDebug;
import static org.ballerinalang.artifactgen.utils.DockerGenUtils.printError;
import static org.ballerinalang.artifactgen.utils.DockerGenUtils.printInfo;
import static org.ballerinalang.artifactgen.utils.DockerGenUtils.printInstruction;
import static org.ballerinalang.artifactgen.utils.DockerGenUtils.printSuccess;

/**
 * Process Docker Annotations.
 */
class DockerAnnotationProcessor {

    private static final String BALX = ".balx";
    private static final String DEFAULT_BASE_IMAGE = "ballerina/ballerina:latest";
    private static final int DEFAULT_DEBUG_PORT = 5005;

    /**
     * Process docker annotations for ballerina Service.
     *
     * @param serviceInfo  ServiceInfo Object
     * @param balxFilePath ballerina file name
     * @param outputDir    target output directory
     */
    void processDockerAnnotationForService(ServiceInfo serviceInfo, String balxFilePath, String
            outputDir) {
        AnnAttachmentInfo dockerAnnotationInfo = serviceInfo.getAnnotationAttachmentInfo
                (DockerGenConstants.DOCKER_ANNOTATION_PACKAGE, DockerGenConstants.DOCKER_ANNOTATION);
        if (dockerAnnotationInfo == null) {
            return;
        }
        //create Docker model from annotations.
        DockerModel dockerModel = new DockerModel();
        //TODO: Handle functions.
        dockerModel.setService(true);
        String balxFileName = DockerGenUtils.extractBalxName(balxFilePath) + BALX;
        dockerModel.setBalxFileName(balxFileName);
        dockerModel.setBalxFilePath(balxFilePath);
        String tag = dockerAnnotationInfo.getAttributeValue(DockerGenConstants.DOCKER_TAG) != null ?
                dockerAnnotationInfo.getAttributeValue(DockerGenConstants.DOCKER_TAG).getStringValue() :
                DockerGenConstants.DOCKER_TAG_LATEST;
        dockerModel.setTag(tag);

        String registry = dockerAnnotationInfo.getAttributeValue(DockerGenConstants.DOCKER_REGISTRY) != null ?
                dockerAnnotationInfo.getAttributeValue(DockerGenConstants.DOCKER_REGISTRY).getStringValue() : null;
        dockerModel.setRegistry(registry);

        String username = dockerAnnotationInfo.getAttributeValue(DockerGenConstants
                .DOCKER_USERNAME) != null ? dockerAnnotationInfo.getAttributeValue(DockerGenConstants
                .DOCKER_USERNAME).getStringValue() : null;
        dockerModel.setUsername(username);

        String password = dockerAnnotationInfo.getAttributeValue(DockerGenConstants
                .DOCKER_PASSWORD) != null ? dockerAnnotationInfo.getAttributeValue(DockerGenConstants
                .DOCKER_PASSWORD).getStringValue() : null;
        dockerModel.setPassword(password);

        boolean imageBuild = dockerAnnotationInfo.getAttributeValue(DockerGenConstants
                .DOCKER_IMAGE_BUILD) == null || dockerAnnotationInfo.getAttributeValue(DockerGenConstants
                .DOCKER_IMAGE_BUILD).getBooleanValue();
        dockerModel.setBuildImage(imageBuild);

        boolean push = dockerAnnotationInfo.getAttributeValue(DockerGenConstants
                .DOCKER_PUSH) != null && dockerAnnotationInfo.getAttributeValue(DockerGenConstants
                .DOCKER_PUSH).getBooleanValue();
        dockerModel.setPush(push);

        String baseImage = dockerAnnotationInfo.getAttributeValue(DockerGenConstants
                .DOCKER_BASE_IMAGE) != null ? dockerAnnotationInfo.getAttributeValue(DockerGenConstants
                .DOCKER_BASE_IMAGE).getStringValue() : DEFAULT_BASE_IMAGE;
        dockerModel.setBaseImage(baseImage);

        boolean debugEnable = dockerAnnotationInfo.getAttributeValue(DockerGenConstants
                .DOCKER_DEBUG_ENABLE) != null && dockerAnnotationInfo.getAttributeValue(DockerGenConstants
                .DOCKER_DEBUG_ENABLE).getBooleanValue();
        List<Integer> ports = extractPorts(serviceInfo);
        dockerModel.setDebugEnable(debugEnable);
        if (debugEnable) {
            int debugPort = dockerAnnotationInfo.getAttributeValue(DockerGenConstants.DOCKER_DEBUG_PORT) != null ?
                    Math.toIntExact(dockerAnnotationInfo.getAttributeValue(DockerGenConstants.DOCKER_DEBUG_PORT)
                            .getIntValue()) : DEFAULT_DEBUG_PORT;
            dockerModel.setDebugPort(debugPort);
            ports.add(debugPort);
        }
        dockerModel.setPorts(ports);
        String nameValue = dockerAnnotationInfo.getAttributeValue(DockerGenConstants.DOCKER_NAME) != null ?
                dockerAnnotationInfo.getAttributeValue(DockerGenConstants.DOCKER_NAME).getStringValue() :
                DockerGenUtils.extractBalxName(balxFilePath);
        nameValue = (registry != null) ? registry + "/" + nameValue + ":" + tag : nameValue + ":" + tag;
        dockerModel.setName(nameValue);

        printDebug(dockerModel.toString());
        createDockerArtifacts(dockerModel, balxFilePath, outputDir);
        printDockerInstructions(dockerModel);
    }


    private void createDockerArtifacts(DockerModel dockerModel, String balxFilePath, String outputDir) {
        DockerArtifactHandler dockerArtifactHandler = new DockerArtifactHandler(dockerModel);
        String dockerContent = dockerArtifactHandler.generate();
        try {
            printInfo("Creating Dockerfile ...");
            DockerGenUtils.writeToFile(dockerContent, outputDir + File.separator + "Dockerfile");
            printSuccess("Dockerfile generated.");
            String balxDestination = outputDir + File.separator + DockerGenUtils.extractBalxName
                    (balxFilePath) + BALX;
            DockerGenUtils.copyFile(balxFilePath, balxDestination);
            //check image build is enabled.
            if (dockerModel.isBuildImage()) {
                printInfo("Creating docker image ...");
                dockerArtifactHandler.buildImage(dockerModel.getName(), outputDir);
                Files.delete(Paths.get(balxDestination));
                //push only if image build is enabled.
                if (dockerModel.isPush()) {
                    dockerArtifactHandler.pushImage(dockerModel);
                }
            }
        } catch (IOException e) {
            printError("Unable to write Dockerfile content to " + outputDir);
        } catch (InterruptedException e) {
            printError("Unable to create docker images " + e.getMessage());
        }
    }


    private void printDockerInstructions(DockerModel dockerModel) {
        printInstruction("\nRun following command to start docker container: ");
        StringBuilder command = new StringBuilder("docker run -d ");
        dockerModel.getPorts().forEach((Integer port) -> command.append("-p ").append(port).append(":").append(port)
                .append(" "));
        command.append(dockerModel.getName());
        printInstruction(command.toString());
    }

}
