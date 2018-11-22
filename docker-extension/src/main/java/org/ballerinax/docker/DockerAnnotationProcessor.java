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
import org.ballerinax.docker.generator.DockerArtifactHandler;
import org.ballerinax.docker.generator.exceptions.DockerGenException;
import org.ballerinax.docker.generator.models.CopyFileModel;
import org.ballerinax.docker.generator.models.DockerModel;
import org.ballerinax.docker.models.DockerDataHolder;
import org.ballerinax.docker.utils.DockerPluginUtils;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangArrayLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.ballerinax.docker.generator.DockerGenConstants.BALX;
import static org.ballerinax.docker.generator.DockerGenConstants.REGISTRY_SEPARATOR;
import static org.ballerinax.docker.generator.DockerGenConstants.TAG_SEPARATOR;
import static org.ballerinax.docker.utils.DockerPluginUtils.isBlank;
import static org.ballerinax.docker.utils.DockerPluginUtils.printDebug;
import static org.ballerinax.docker.utils.DockerPluginUtils.resolveValue;

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
        try {
            DockerModel dockerModel = dockerDataHolder.getDockerModel();
            dockerModel.setPorts(dockerDataHolder.getPorts());
            dockerModel.setCopyFiles(dockerDataHolder.getExternalFiles());
            // set docker image name
            if (dockerModel.getName() == null) {
                String defaultImageName = DockerPluginUtils.extractBalxName(balxFilePath);
                dockerModel.setName(defaultImageName);
            }
            String registry = dockerModel.getRegistry();
            String imageName = dockerModel.getName();
            imageName = (registry != null) ? registry + REGISTRY_SEPARATOR + imageName + TAG_SEPARATOR
                    + dockerModel.getTag() : imageName + TAG_SEPARATOR + dockerModel.getTag();
            dockerModel.setName(imageName);
            dockerModel.setBalxFileName(DockerPluginUtils.extractBalxName(balxFilePath) + BALX);
        
            Set<Integer> ports = dockerModel.getPorts();
            if (dockerModel.isEnableDebug()) {
                ports.add(dockerModel.getDebugPort());
            }
            dockerModel.setPorts(ports);
            printDebug(dockerModel.toString());
            DockerArtifactHandler dockerHandler = new DockerArtifactHandler(dockerModel);
            dockerHandler.createArtifacts(out, "\t@docker \t\t", balxFilePath, outputDir);
            printDockerInstructions(dockerModel);
        } catch (DockerGenException e) {
            throw new DockerPluginException("Error occurred when trying to create/build/push docker image.",
                    e);
        }
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
        String dockerHost = System.getenv(DockerPluginConstants.DOCKER_HOST);
        if (!isBlank(dockerHost)) {
            dockerModel.setDockerHost(dockerHost);
        }
        String dockerCertPath = System.getenv(DockerPluginConstants.DOCKER_CERT_PATH);
        if (!isBlank(dockerCertPath)) {
            dockerModel.setDockerCertPath(dockerCertPath);
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
    
    private void printDockerInstructions(DockerModel dockerModel) {
        out.println();
        out.println("\n\tRun the following command to start a Docker container:");
        StringBuilder command = new StringBuilder("docker run -d ");
        dockerModel.getPorts().forEach((Integer port) -> command.append("-p ").append(port).append(":").append(port)
                .append(" "));
        command.append(dockerModel.getName());
        out.println("\t" + command.toString());
        out.println();
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
