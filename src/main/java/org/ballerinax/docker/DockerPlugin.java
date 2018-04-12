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
import org.ballerinalang.model.tree.EndpointNode;
import org.ballerinalang.model.tree.ServiceNode;
import org.ballerinalang.model.tree.expressions.RecordLiteralNode;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.models.DockerDataHolder;
import org.ballerinax.docker.utils.DockerGenUtils;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.ballerinax.docker.DockerGenConstants.LISTENER;
import static org.ballerinax.docker.utils.DockerGenUtils.printError;

/**
 * Compiler plugin to generate docker artifacts.
 */
@SupportedAnnotationPackages(
        value = "ballerinax.docker"
)
public class DockerPlugin extends AbstractCompilerPlugin {
    private static boolean canProcess;
    private DockerAnnotationProcessor dockerAnnotationProcessor;
    private DiagnosticLog dlog;

    private static synchronized void setCanProcess(boolean val) {
        canProcess = val;
    }

    @Override
    public void init(DiagnosticLog diagnosticLog) {
        this.dlog = diagnosticLog;
        dockerAnnotationProcessor = new DockerAnnotationProcessor();
        setCanProcess(false);
    }

    @Override
    public void process(ServiceNode serviceNode, List<AnnotationAttachmentNode> annotations) {
        setCanProcess(true);
        try {
            for (AnnotationAttachmentNode attachmentNode : annotations) {
                String annotationKey = attachmentNode.getAnnotationName().getValue();
                switch (annotationKey) {
                    case "Config":
                        DockerDataHolder.getInstance().setDockerModel(
                                dockerAnnotationProcessor.processConfigAnnotation(attachmentNode));
                        break;
                    case "CopyFiles":
                        DockerDataHolder.getInstance().addExternalFile(
                                dockerAnnotationProcessor.processCopyFileAnnotation(attachmentNode));
                        break;
                    default:
                        break;
                }
            }
            RecordLiteralNode endpointConfig = serviceNode.getAnonymousEndpointBind();
            if (endpointConfig != null) {
                List<BLangRecordLiteral.BLangRecordKeyValue> config =
                        ((BLangRecordLiteral) endpointConfig).getKeyValuePairs();
                DockerDataHolder.getInstance().addPort(extractPort(config));
            }
        } catch (DockerPluginException e) {
            dlog.logDiagnostic(Diagnostic.Kind.ERROR, serviceNode.getPosition(), e.getMessage());
        }
    }

    @Override
    public void process(EndpointNode endpointNode, List<AnnotationAttachmentNode> annotations) {
        setCanProcess(true);
        if (!LISTENER.equals(endpointNode.getEndPointType().getTypeName().getValue())) {
            dlog.logDiagnostic(Diagnostic.Kind.ERROR, endpointNode.getPosition(), "@docker " +
                    "annotations are only supported by Listener endpoints.");
            //TODO: Remove return when dlog fixed.
            return;
        }
        try {
            for (AnnotationAttachmentNode attachmentNode : annotations) {
                String annotationKey = attachmentNode.getAnnotationName().getValue();
                switch (annotationKey) {
                    case "Config":
                        DockerDataHolder.getInstance().setDockerModel(
                                dockerAnnotationProcessor.processConfigAnnotation(attachmentNode));
                        break;
                    case "CopyFiles":
                        DockerDataHolder.getInstance().addExternalFile(
                                dockerAnnotationProcessor.processCopyFileAnnotation(attachmentNode));
                        break;
                    case "Expose":
                        List<BLangRecordLiteral.BLangRecordKeyValue> config =
                                ((BLangRecordLiteral) endpointNode.getConfigurationExpression()).getKeyValuePairs();
                        DockerDataHolder.getInstance().addPort(extractPort(config));
                        break;
                    default:
                        break;
                }
            }
        } catch (DockerPluginException e) {
            dlog.logDiagnostic(Diagnostic.Kind.ERROR, endpointNode.getPosition(), e.getMessage());
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
                DockerGenUtils.deleteDirectory(targetPath);
                dockerAnnotationProcessor.processDockerModel(DockerDataHolder.getInstance(), filePath, targetPath);
            } catch (DockerPluginException e) {
                printError(e.getMessage());
                dlog.logDiagnostic(Diagnostic.Kind.ERROR, null, e.getMessage());
                try {
                    DockerGenUtils.deleteDirectory(targetPath);
                } catch (DockerPluginException ignored) {
                }
            }
        }
    }


    private int extractPort(List<BLangRecordLiteral.BLangRecordKeyValue> endpointConfig) throws
            DockerPluginException {
        for (BLangRecordLiteral.BLangRecordKeyValue keyValue : endpointConfig) {
            String key = keyValue.getKey().toString();
            if ("port".equals(key)) {
                return Integer.parseInt(keyValue.getValue().toString());
            }
        }
        throw new DockerPluginException("Unable to extract port from anonymous endpoint");
    }
}
