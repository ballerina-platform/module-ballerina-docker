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
import org.ballerinalang.model.tree.NodeKind;
import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.generator.DockerArtifactHandler;
import org.ballerinax.docker.generator.exceptions.DockerGenException;
import org.ballerinax.docker.generator.models.CopyFileModel;
import org.ballerinax.docker.generator.models.DockerModel;
import org.ballerinax.docker.generator.utils.DockerGenUtils;
import org.ballerinax.docker.models.DockerContext;
import org.ballerinax.docker.models.DockerDataHolder;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BConstantSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.types.BFiniteType;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.ballerinax.docker.utils.DockerPluginUtils.getKeyValuePairs;
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
     * @param uberJarFilePath Uber jar file name
     * @param outputDir       target output directory
     */
    void processDockerModel(DockerDataHolder dockerDataHolder, Path uberJarFilePath, Path outputDir) throws
            DockerPluginException {
        try {
            DockerModel dockerModel = dockerDataHolder.getDockerModel();
            dockerModel.setPorts(dockerDataHolder.getPorts());
            dockerModel.setCopyFiles(dockerDataHolder.getExternalFiles());
            // set docker image name
            if (dockerModel.getName() == null) {
                String defaultImageName = DockerGenUtils.extractUberJarName(uberJarFilePath);
                dockerModel.setName(defaultImageName);
            }

            Path uberJarFileName = uberJarFilePath.getFileName();
            if (null != uberJarFileName) {
                dockerModel.setUberJarFileName(uberJarFileName.toString());
            }

            Set<Integer> ports = dockerModel.getPorts();
            if (dockerModel.isEnableDebug()) {
                ports.add(dockerModel.getDebugPort());
            }
            dockerModel.setPorts(ports);
            printDebug(dockerModel.toString());
            out.println("\nGenerating docker artifacts...");
            DockerArtifactHandler dockerHandler = new DockerArtifactHandler(dockerModel);
            dockerHandler.createArtifacts(out, "\t@docker \t\t", uberJarFilePath, outputDir);
            printDockerInstructions(dockerModel, outputDir);
        } catch (DockerGenException e) {
            throw new DockerPluginException(e.getMessage(), e);
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
        List<BLangRecordLiteral.BLangRecordKeyValueField> keyValues =
                getKeyValuePairs((BLangRecordLiteral) ((BLangAnnotationAttachment) attachmentNode).expr);
        DockerModel dockerModel = DockerContext.getInstance().getDataHolder().getDockerModel();
        for (BLangRecordLiteral.BLangRecordKeyValueField keyValue : keyValues) {
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
                case buildImage:
                    dockerModel.setBuildImage(Boolean.parseBoolean(annotationValue));
                    break;
                case push:
                    dockerModel.setPush(Boolean.parseBoolean(annotationValue));
                    break;
                case cmd:
                    dockerModel.setCmd(annotationValue.trim());
                    break;
                case enableDebug:
                    dockerModel.setEnableDebug(Boolean.parseBoolean(annotationValue));
                    break;
                case debugPort:
                    dockerModel.setDebugPort(Integer.parseInt(annotationValue));
                    break;
                case dockerAPIVersion:
                    dockerModel.setDockerAPIVersion(annotationValue);
                    break;
                case dockerHost:
                    dockerModel.setDockerHost(annotationValue);
                    break;
                case dockerCertPath:
                    dockerModel.setDockerCertPath(annotationValue);
                    break;
                case env:
                    dockerModel.setEnv(getMap(keyValue.getValue()));
                    break;
                case dockerConfigPath:
                    dockerModel.setDockerConfig(annotationValue);
                    break;
                default:
                    break;
            }
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
        List<BLangRecordLiteral.BLangRecordKeyValueField> keyValues =
                getKeyValuePairs((BLangRecordLiteral) ((BLangAnnotationAttachment) attachmentNode).expr);
        for (BLangRecordLiteral.BLangRecordKeyValueField keyValue : keyValues) {
            List<BLangExpression> configAnnotation = ((BLangListConstructorExpr) keyValue.valueExpr).exprs;
            for (BLangExpression bLangExpression : configAnnotation) {
                CopyFileModel fileModel = new CopyFileModel();
                List<BLangRecordLiteral.BLangRecordKeyValueField> annotationValues =
                        getKeyValuePairs((BLangRecordLiteral) bLangExpression);
                for (BLangRecordLiteral.BLangRecordKeyValueField annotation : annotationValues) {
                    CopyFileConfiguration copyFileConfiguration =
                            CopyFileConfiguration.valueOf(annotation.getKey().toString());
                    String annotationValue = resolveValue(annotation.getValue().toString());
                    switch (copyFileConfiguration) {
                        case sourceFile:
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

    private void printDockerInstructions(DockerModel dockerModel, Path outputDir) {
        out.println();

        if (!dockerModel.isBuildImage()) {
            out.println();
            out.println("\tRun the following command to build the docker image:");
            String command = "docker build --force-rm --no-cache -t " + dockerModel.getName();
            Path currentDir = Paths.get(System.getProperty("user.dir"));
            command += " " + currentDir.relativize(outputDir);
            out.println("\t" + command);
        }

        out.println();
        out.println("\tRun the following command to start a Docker container:");
        StringBuilder command = new StringBuilder("docker run -d ");
        dockerModel.getPorts().forEach((Integer port) -> command.append("-p ").append(port).append(":").append(port)
                .append(" "));
        command.append(dockerModel.getName());
        out.println("\t" + command.toString());
        out.println();
    }

    /**
     * Get a map from a ballerina expression.
     *
     * @param expr Ballerina record value.
     * @return Map of key values.
     * @throws DockerPluginException When the expression cannot be parsed.
     */
    private Map<String, String> getMap(BLangExpression expr) throws DockerPluginException {
        if (expr.getKind() != NodeKind.RECORD_LITERAL_EXPR) {
            throw new DockerPluginException("unable to parse value: " + expr.toString());
        } else {
            BLangRecordLiteral fields = (BLangRecordLiteral) expr;
            Map<String, String> map = new LinkedHashMap<>();
            for (BLangRecordLiteral.BLangRecordKeyValueField keyValue : convertRecordFields(fields.getFields())) {
                map.put(keyValue.getKey().toString(), getStringValue(keyValue.getValue()));
            }
            return map;
        }
    }

    private List<BLangRecordLiteral.BLangRecordKeyValueField> convertRecordFields(
            List<BLangRecordLiteral.RecordField> fields) {
        return fields.stream().map(f -> (BLangRecordLiteral.BLangRecordKeyValueField) f).collect(Collectors.toList());
    }

    private String getStringValue(BLangExpression expr) throws DockerPluginException {
        if (expr instanceof BLangSimpleVarRef) {
            BLangSimpleVarRef varRef = (BLangSimpleVarRef) expr;
            if (varRef.symbol instanceof BConstantSymbol) {
                BConstantSymbol constantSymbol = (BConstantSymbol) varRef.symbol;
                if (constantSymbol.type instanceof BFiniteType) {
                    // Parse compile time constant
                    BFiniteType compileConst = (BFiniteType) constantSymbol.type;
                    if (compileConst.getValueSpace().size() > 0) {
                        return resolveValue(compileConst.getValueSpace().iterator().next().toString());
                    }
                }
            }
        } else if (expr instanceof BLangLiteral) {
            return resolveValue(expr.toString());
        }
        throw new DockerPluginException("unable to parse value: " + expr.toString());
    }

    private enum DockerConfiguration {
        name,
        registry,
        tag,
        username,
        password,
        baseImage,
        buildImage,
        push,
        cmd,
        enableDebug,
        debugPort,
        dockerAPIVersion,
        dockerHost,
        dockerCertPath,
        env,
        dockerConfigPath
    }

    private enum CopyFileConfiguration {
        sourceFile,
        target,
        isBallerinaConf
    }
}
