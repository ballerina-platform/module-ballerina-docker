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
import org.ballerinalang.model.tree.PackageNode;
import org.ballerinalang.model.tree.ServiceNode;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.models.DockerDataHolder;
import org.ballerinax.docker.utils.DockerGenUtils;
import org.wso2.ballerinalang.compiler.tree.BLangEndpoint;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangRecordKeyValue;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.ballerinax.docker.utils.DockerGenUtils.printError;

/**
 * Compiler plugin to generate docker artifacts.
 */
@SupportedAnnotationPackages(
        value = "ballerinax.docker"
)
public class DockerPlugin extends AbstractCompilerPlugin {
    private static boolean canProcess;
    private static DockerDataHolder dockerDataHolder = new DockerDataHolder();
    ;
    private DockerAnnotationProcessor dockerAnnotationProcessor;
    private DiagnosticLog dlog;

    private static synchronized void setCanProcess(boolean val) {
        canProcess = val;
    }

    @Override
    public void init(DiagnosticLog diagnosticLog) {
        this.dlog = diagnosticLog;
        dockerAnnotationProcessor = new DockerAnnotationProcessor();
        dockerAnnotationProcessor = new DockerAnnotationProcessor();
        setCanProcess(false);
    }

    @Override
    public void process(PackageNode packageNode) {
        // extract port values from services.
        List<? extends EndpointNode> endpointNodes = packageNode.getGlobalEndpoints();
        for (EndpointNode endpointNode : endpointNodes) {
            List<BLangRecordKeyValue> keyValues = ((BLangRecordLiteral)
                    ((BLangEndpoint) endpointNode).configurationExpr).getKeyValuePairs();
            keyValues.forEach(keyValue -> {
                if ("port".equals(keyValue.getKey().toString())) {
                    dockerDataHolder.addPort(Integer.parseInt(keyValue.getValue().toString()));
                }
            });
        }
    }

    @Override
    public void process(ServiceNode serviceNode, List<AnnotationAttachmentNode> annotations) {
        setCanProcess(true);
        try {
            dockerDataHolder.setDockerModel(dockerAnnotationProcessor.processDockerAnnotation(annotations));
        } catch (DockerPluginException e) {
            dlog.logDiagnostic(Diagnostic.Kind.ERROR, serviceNode.getPosition(), e.getMessage());
        }
    }

    @Override
    public void process(EndpointNode endpointNode, List<AnnotationAttachmentNode> annotations) {
        setCanProcess(true);
        try {
            dockerDataHolder.setDockerModel(dockerAnnotationProcessor.processDockerAnnotation(annotations));
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
                dockerAnnotationProcessor.processDockerModel(dockerDataHolder, filePath, targetPath);
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


}
