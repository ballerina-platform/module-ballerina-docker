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

import org.ballerinalang.compiler.plugins.AbstractCompilerPlugin;
import org.ballerinalang.compiler.plugins.SupportedAnnotationPackages;
import org.ballerinalang.model.tree.AnnotationAttachmentNode;
import org.ballerinalang.model.tree.Node;
import org.ballerinalang.model.tree.PackageNode;
import org.ballerinalang.model.tree.ServiceNode;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.ballerinax.docker.exceptions.DockerPluginException;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangAnnotAttachmentAttribute;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangRecordKeyValue;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.ballerinax.docker.DockerAnnotationProcessor.dockerModel;
import static org.ballerinax.docker.DockerGenConstants.BALLERINA_NET_HTTP;
import static org.ballerinax.docker.utils.DockerGenUtils.printDebug;
import static org.ballerinax.docker.utils.DockerGenUtils.printError;

/**
 * Compiler plugin to generate docker artifacts.
 */
@SupportedAnnotationPackages(
        value = "ballerinax.docker"
)
public class DockerPlugin extends AbstractCompilerPlugin {
    private static List<Integer> ports = new ArrayList<>();
    private static boolean canProcess;
    private DiagnosticLog dlog;

    private static synchronized void setCanProcess(boolean val) {
        canProcess = val;
    }

    @Override
    public void init(DiagnosticLog diagnosticLog) {
        this.dlog = diagnosticLog;
        setCanProcess(false);
    }

    @Override
    public void process(PackageNode packageNode) {
        // extract port values from services.
        List<? extends ServiceNode> serviceNodes = packageNode.getServices();
        for (ServiceNode serviceNode : serviceNodes) {
            List<? extends AnnotationAttachmentNode> annotationAttachmentNodes = serviceNode.getAnnotationAttachments();
            for (AnnotationAttachmentNode annotationAttachmentNode : annotationAttachmentNodes) {
                String packageID = ((BLangAnnotationAttachment) annotationAttachmentNode).
                        annotationSymbol.pkgID.name.value;
                if (BALLERINA_NET_HTTP.equals(packageID)) {
                    List<BLangAnnotAttachmentAttribute> bLangAnnotationAttachments = ((BLangAnnotationAttachment)
                            annotationAttachmentNode).attributes;
                    for (BLangAnnotAttachmentAttribute annotationAttribute : bLangAnnotationAttachments) {
                        String annotationKey = annotationAttribute.name.value;
                        if ("port".equals(annotationKey)) {
                            Node annotationValue = annotationAttribute.getValue().getValue();
                            ports.add(Integer.parseInt(annotationValue.toString()));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void process(ServiceNode serviceNode, List<AnnotationAttachmentNode> annotations) {
        setCanProcess(true);
        for (AnnotationAttachmentNode attachmentNode : annotations) {
            List<BLangRecordKeyValue> keyValues =
                    ((BLangRecordLiteral) ((BLangAnnotationAttachment) attachmentNode).expr).getKeyValuePairs();
            for (BLangRecordKeyValue keyValue : keyValues) {
                DockerConfiguration dockerConfiguration =
                        DockerConfiguration.valueOf(keyValue.getKey().toString());
                String annotationValue = keyValue.getValue().toString();
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
                    default:
                        break;
                }
            }
            dockerModel.setService(true);
            dockerModel.setPorts(ports);
            printDebug(dockerModel.toString());
        }
    }

    @Override
    public void codeGenerated(Path binaryPath) {
        if (canProcess) {
            String filePath = binaryPath.toAbsolutePath().toString();
            String userDir = new File(filePath).getParentFile().getAbsolutePath();
            String targetPath = userDir + File.separator + "docker" + File.separator;
            DockerAnnotationProcessor dockerAnnotationProcessor = new DockerAnnotationProcessor();
            try {
                dockerAnnotationProcessor.processDockerModel(filePath, targetPath);
            } catch (DockerPluginException e) {
                printError(e.getMessage());
                dlog.logDiagnostic(Diagnostic.Kind.ERROR, null, e.getMessage());
            }
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
        dockerHost
    }
}
