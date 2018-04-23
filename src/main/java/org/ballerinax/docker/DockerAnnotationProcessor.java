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

import org.ballerinalang.model.tree.AnnotationAttachmentNode;
import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.models.CopyFileModel;
import org.ballerinax.docker.models.DockerDataHolder;
import org.ballerinax.docker.models.DockerModel;
import org.ballerinax.docker.utils.DockerGenUtils;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangArrayLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.ballerinax.docker.DockerGenConstants.BALX;
import static org.ballerinax.docker.DockerGenConstants.DEFAULT_DOCKER_HOST;
import static org.ballerinax.docker.DockerGenConstants.REGISTRY_SEPARATOR;
import static org.ballerinax.docker.DockerGenConstants.TAG_SEPARATOR;
import static org.ballerinax.docker.utils.DockerGenUtils.printDebug;
import static org.ballerinax.docker.utils.DockerGenUtils.resolveValue;

/**
 * Process Docker Annotations.
 */
class DockerAnnotationProcessor {

    private PrintStream out = System.out;

    /**
     * Process docker annotations for ballerina Service.
     *
     * @param balxFilePath ballerina file name
     * @param outputDir    target output directory
     */
    void processDockerModel(DockerDataHolder dockerDataHolder, String balxFilePath, String outputDir) throws
            DockerPluginException {
        DockerModel dockerModel = dockerDataHolder.getDockerModel();
        dockerModel.setPorts(dockerDataHolder.getPorts());
        dockerModel.setFiles(dockerDataHolder.getFiles());
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

        Set<Integer> ports = dockerModel.getPorts();
        if (dockerModel.isEnableDebug()) {
            ports.add(dockerModel.getDebugPort());
        }
        dockerModel.setPorts(ports);
        printDebug(dockerModel.toString());
        createDockerArtifacts(dockerModel, balxFilePath, outputDir);
        printDockerInstructions(dockerModel);
    }

    /**
     * Process annotations and generate docker model.
     *
     * @param attachmentNode docker annotation node.
     * @return DockerModel object
     * @throws DockerPluginException if an error occurred while creating docker model
     */
    DockerModel processConfigAnnotation(AnnotationAttachmentNode attachmentNode) throws DockerPluginException {
        List<BLangRecordLiteral.BLangRecordKeyValue> keyValues =
                ((BLangRecordLiteral) ((BLangAnnotationAttachment) attachmentNode).expr).getKeyValuePairs();
        DockerModel dockerModel = new DockerModel();
        for (BLangRecordLiteral.BLangRecordKeyValue keyValue : keyValues) {
            DockerConfiguration dockerConfiguration =
                    DockerConfiguration.valueOf(keyValue.getKey().toString());
            String annotationValue = resolveValue(keyValue.getValue().toString());
            switch (dockerConfiguration) {
                case name:
                    dockerModel.setName(annotationValue);
                    break;
                case registry:
                    dockerModel.setRegistry(annotationValue);
                    break;
                case tag:
                    dockerModel.setTag(annotationValue);
                    break;
                case username:
                    dockerModel.setUsername(annotationValue);
                    break;
                case password:
                    dockerModel.setPassword(annotationValue);
                    break;
                case baseImage:
                    dockerModel.setBaseImage(annotationValue);
                    break;
                case push:
                    dockerModel.setPush(Boolean.valueOf(annotationValue));
                    break;
                case buildImage:
                    dockerModel.setBuildImage(Boolean.valueOf(annotationValue));
                    break;
                case enableDebug:
                    dockerModel.setEnableDebug(Boolean.valueOf(annotationValue));
                    break;
                case debugPort:
                    dockerModel.setDebugPort(Integer.parseInt(annotationValue));
                    break;
                case dockerHost:
                    dockerModel.setDockerHost(annotationValue);
                    break;
                case dockerCertPath:
                    dockerModel.setDockerCertPath(annotationValue);
                    break;
                default:
                    break;
            }
        }
        String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.getDefault());
        if (operatingSystem.contains("win") && DEFAULT_DOCKER_HOST.equals(dockerModel.getDockerHost())) {
            // Windows users must specify docker host
            throw new DockerPluginException("Windows users must specify dockerHost parameter in @docker:Config{} " +
                    "annotation.");
        }
        dockerModel.setService(true);
        return dockerModel;
    }

    /**
     * Process annotations and generate CopyFile model.
     *
     * @param attachmentNode docker annotation node.
     * @return CopyFileModel object set
     * @throws DockerPluginException if an error occurred while creating model
     */
    Set<CopyFileModel> processCopyFileAnnotation(AnnotationAttachmentNode attachmentNode) throws DockerPluginException {
        Set<CopyFileModel> copyFileModels = new HashSet<>();
        // control variable to detect if there are multiple conf files.
        boolean confFileDefined = false;
        List<BLangRecordLiteral.BLangRecordKeyValue> keyValues =
                ((BLangRecordLiteral) ((BLangAnnotationAttachment) attachmentNode).expr).getKeyValuePairs();
        for (BLangRecordLiteral.BLangRecordKeyValue keyValue : keyValues) {
            List<BLangExpression> configAnnotation = ((BLangArrayLiteral) keyValue.valueExpr).exprs;
            for (BLangExpression bLangExpression : configAnnotation) {
                CopyFileModel fileModel = new CopyFileModel();
                List<BLangRecordLiteral.BLangRecordKeyValue> annotationValues =
                        ((BLangRecordLiteral) bLangExpression).getKeyValuePairs();
                for (BLangRecordLiteral.BLangRecordKeyValue annotation : annotationValues) {
                    CopyFileConfiguration copyFileConfiguration =
                            CopyFileConfiguration.valueOf(annotation.getKey().toString());
                    String annotationValue = resolveValue(annotation.getValue().toString());
                    switch (copyFileConfiguration) {
                        case source:
                            fileModel.setSource(annotationValue);
                            break;
                        case target:
                            fileModel.setTarget(annotationValue);
                            break;
                        case isBallerinaConf:
                            boolean value = Boolean.parseBoolean(annotationValue);
                            if (confFileDefined && value) {
                                throw new DockerPluginException("@docker:CopyFiles{} annotation has more than one " +
                                        "conf files defined.");
                            }
                            fileModel.setBallerinaConf(value);
                            confFileDefined = Boolean.parseBoolean(annotationValue);
                            break;
                        default:
                            break;
                    }
                }
                copyFileModels.add(fileModel);
            }
        }
        return copyFileModels;
    }

    private void createDockerArtifacts(DockerModel dockerModel, String balxFilePath, String outputDir) throws
            DockerPluginException {
        DockerArtifactHandler dockerArtifactHandler = new DockerArtifactHandler(dockerModel);
        out.print("@docker \t\t - complete 1/3 \r");
        String dockerContent = dockerArtifactHandler.generate();
        try {
            DockerGenUtils.writeToFile(dockerContent, outputDir + File.separator + "Dockerfile");
            String balxDestination = outputDir + File.separator + DockerGenUtils.extractBalxName
                    (balxFilePath) + BALX;
            copyFile(balxFilePath, balxDestination);
            for (CopyFileModel copyFileModel : dockerModel.getFiles()) {
                // Copy external files to docker folder
                String target = outputDir + File.separator + String.valueOf(Paths.get(copyFileModel.getSource())
                        .getFileName());
                copyFile(copyFileModel.getSource(), target);

            }
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
            throw new DockerPluginException("Unable to write content to " + outputDir);
        } catch (InterruptedException e) {
            throw new DockerPluginException("Unable to create docker images " + e.getMessage());
        }
    }


    private void printDockerInstructions(DockerModel dockerModel) {
        String ansiReset = "\u001B[0m";
        String ansiCyan = "\u001B[36m";
        out.println();
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

    private enum DockerConfiguration {
        name,
        registry,
        tag,
        username,
        password,
        baseImage,
        push,
        buildImage,
        enableDebug,
        debugPort,
        dockerHost,
        dockerCertPath
    }

    private enum CopyFileConfiguration {
        source,
        target,
        isBallerinaConf
    }
}
