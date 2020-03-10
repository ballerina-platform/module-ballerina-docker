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
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.tree.AnnotationAttachmentNode;
import org.ballerinalang.model.tree.FunctionNode;
import org.ballerinalang.model.tree.PackageNode;
import org.ballerinalang.model.tree.ServiceNode;
import org.ballerinalang.model.tree.SimpleVariableNode;
import org.ballerinalang.model.tree.TopLevelNode;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.generator.models.CopyFileModel;
import org.ballerinax.docker.models.DockerContext;
import org.ballerinax.docker.models.DockerDataHolder;
import org.ballerinax.docker.utils.DockerPluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangService;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangNamedArgsExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeInit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.ballerinax.docker.generator.DockerGenConstants.ARTIFACT_DIRECTORY;
import static org.ballerinax.docker.generator.utils.DockerGenUtils.extractUberJarName;
import static org.ballerinax.docker.utils.DockerPluginUtils.createAnnotation;
import static org.ballerinax.docker.utils.DockerPluginUtils.getKeyValuePairs;
import static org.ballerinax.docker.utils.DockerPluginUtils.printError;

/**
 * Compiler plugin to generate docker artifacts.
 */
@SupportedAnnotationPackages(
        value = "ballerina/docker"
)
public class DockerPlugin extends AbstractCompilerPlugin {
    private static final Logger pluginLog = LoggerFactory.getLogger(DockerPlugin.class);
    private DockerAnnotationProcessor dockerAnnotationProcessor;
    private DiagnosticLog dlog;

    @Override
    public void init(DiagnosticLog diagnosticLog) {
        this.dlog = diagnosticLog;
        this.dockerAnnotationProcessor = new DockerAnnotationProcessor();
    }

    @Override
    public void process(PackageNode packageNode) {
        BLangPackage bPackage = (BLangPackage) packageNode;
        String pkgID = bPackage.packageID.toString();
        DockerContext.getInstance().addDataHolder(pkgID);
    
        // Get the imports with alias _
        List<BLangImportPackage> dockerImports = bPackage.getImports().stream()
                .filter(i -> i.symbol.toString().equals("ballerina/docker") && i.getAlias().toString().equals("_"))
                .collect(Collectors.toList());
    
        if (dockerImports.size() > 0) {
            for (BLangImportPackage dockerImport : dockerImports) {
                // Get the units of the file which has docker import as _
                List<TopLevelNode> topLevelNodes = bPackage.getCompilationUnits().stream()
                        .filter(cu -> cu.getName().equals(dockerImport.compUnit.getValue()))
                        .flatMap(cu -> cu.getTopLevelNodes().stream())
                        .collect(Collectors.toList());
    
                // Filter out the services
                List<ServiceNode> serviceNodes = topLevelNodes.stream()
                        .filter(tln -> tln instanceof ServiceNode)
                        .map(tln -> (ServiceNode) tln)
                        .collect(Collectors.toList());
        
                // Generate artifacts for services for all services
                serviceNodes.forEach(sn -> process(sn, Collections.singletonList(createAnnotation("Config"))));
        
                // Get the variable names of the listeners attached to services
                List<String> listenerNamesToExpose = serviceNodes.stream()
                        .map(ServiceNode::getAttachedExprs)
                        .flatMap(Collection::stream)
                        .filter(aex -> aex instanceof BLangSimpleVarRef)
                        .map(aex -> (BLangSimpleVarRef) aex)
                        .map(BLangSimpleVarRef::toString)
                        .collect(Collectors.toList());
    
                // Generate artifacts for listeners attached to services
                topLevelNodes.stream()
                        .filter(tln -> tln instanceof SimpleVariableNode)
                        .map(tln -> (SimpleVariableNode) tln)
                        .filter(listener -> listenerNamesToExpose.contains(listener.getName().getValue()))
                        .forEach(listener -> process(listener, Collections.singletonList(createAnnotation("Expose"))));
                
                // Generate artifacts for main functions
                topLevelNodes.stream()
                        .filter(tln -> tln instanceof FunctionNode)
                        .map(tln -> (FunctionNode) tln)
                        .filter(fn -> "main".equals(fn.getName().getValue()))
                        .forEach(fn -> process(fn, Collections.singletonList(createAnnotation("Config"))));
            }
        }
    }

    @Override
    public void process(ServiceNode serviceNode, List<AnnotationAttachmentNode> annotations) {
        DockerDataHolder dataHolder = DockerContext.getInstance().getDataHolder();
        dataHolder.setCanProcess(true);
        try {
            processCommonAnnotations(annotations, dataHolder);
            BLangService bService = (BLangService) serviceNode;
            for (BLangExpression attachedExpr : bService.getAttachedExprs()) {
                if (attachedExpr instanceof BLangTypeInit) {
                    BLangTypeInit bListener = (BLangTypeInit) attachedExpr;
                    try {
                        dataHolder.addPort(Integer.parseInt(bListener.argsExpr.get(0).toString()));
                    } catch (NumberFormatException e) {
                        throw new DockerPluginException("Unable to parse port of the listener: " +
                                bListener.argsExpr.get(0).toString());
                    }
                }
            }
        } catch (DockerPluginException e) {
            dlog.logDiagnostic(Diagnostic.Kind.ERROR, serviceNode.getPosition(), e.getMessage());
        }
    }

    @Override
    public void process(SimpleVariableNode variableNode, List<AnnotationAttachmentNode> annotations) {
        DockerDataHolder dataHolder = DockerContext.getInstance().getDataHolder();
        dataHolder.setCanProcess(true);
        try {
            for (AnnotationAttachmentNode attachmentNode : annotations) {
                DockerAnnotation dockerAnnotation = DockerAnnotation.valueOf(attachmentNode.getAnnotationName()
                        .getValue());
                switch (dockerAnnotation) {
                    case Config:
                        dataHolder.setDockerModel(
                                dockerAnnotationProcessor.processConfigAnnotation(attachmentNode));
                        break;
                    case CopyFiles:
                        dataHolder.addExternalFiles(
                                dockerAnnotationProcessor.processCopyFileAnnotation(attachmentNode));
                        break;
                    case Expose:
                        BLangTypeInit bListener = (BLangTypeInit) ((BLangSimpleVariable) variableNode).expr;
                        try {
                            dataHolder.addPort(Integer.parseInt(bListener.argsExpr.get(0).toString()));
                            processListener(bListener, dataHolder);
                        } catch (NumberFormatException e) {
                            throw new DockerPluginException("unable to parse port of the service: " +
                                    bListener.argsExpr.get(0).toString());
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (DockerPluginException e) {
            dlog.logDiagnostic(Diagnostic.Kind.ERROR, variableNode.getPosition(), e.getMessage());
        }
    }

    @Override
    public void process(FunctionNode functionNode, List<AnnotationAttachmentNode> annotations) {
        if (!"main".equals(functionNode.getName().getValue())) {
            dlog.logDiagnostic(Diagnostic.Kind.ERROR, functionNode.getPosition(), "@docker annotations are " +
                    "only supported with main function. ");
            return;
        }
        DockerDataHolder dataHolder = DockerContext.getInstance().getDataHolder();
        dataHolder.setCanProcess(true);
        try {
            processCommonAnnotations(annotations, dataHolder);
        } catch (DockerPluginException e) {
            dlog.logDiagnostic(Diagnostic.Kind.ERROR, functionNode.getPosition(), e.getMessage());
        }
    }

    private void processCommonAnnotations(List<AnnotationAttachmentNode> annotations, DockerDataHolder dataHolder)
            throws DockerPluginException {
        for (AnnotationAttachmentNode attachmentNode : annotations) {
            DockerAnnotation dockerAnnotation = DockerAnnotation.valueOf(attachmentNode.getAnnotationName()
                    .getValue());
            switch (dockerAnnotation) {
                case Config:
                    dataHolder.setDockerModel(dockerAnnotationProcessor.processConfigAnnotation(attachmentNode));
                    break;
                case CopyFiles:
                    dataHolder.addExternalFiles(dockerAnnotationProcessor.processCopyFileAnnotation(attachmentNode));
                    break;
                default:
                    break;
            }
        }
    }

    private void processListener(BLangTypeInit bListener, DockerDataHolder dataHolder) {
        List<BLangRecordLiteral.BLangRecordKeyValueField> listenerConfig;
        if (bListener.argsExpr.size() == 2) {
            if (bListener.argsExpr.get(1) instanceof BLangRecordLiteral) {
                BLangRecordLiteral bConfigRecordLiteral = (BLangRecordLiteral) bListener.argsExpr.get(1);
                listenerConfig = bConfigRecordLiteral.getFields().stream().map(x ->
                        (BLangRecordLiteral.BLangRecordKeyValueField) x).collect(Collectors.toList());
            } else {
                // expression is in config = {} format.
                listenerConfig = getKeyValuePairs((BLangRecordLiteral) ((BLangNamedArgsExpression)
                        bListener.argsExpr.get(1)).expr);

            }
            for (BLangRecordLiteral.BLangRecordKeyValueField keyValue : listenerConfig) {
                String key = keyValue.getKey().toString();
                if ("secureSocket".equals(key)) {
                    List<BLangRecordLiteral.BLangRecordKeyValueField> sslKeyValues =
                            getKeyValuePairs((BLangRecordLiteral) keyValue.valueExpr);
                    dataHolder.addExternalFiles(processSecureSocket(sslKeyValues));
                }
            }
        }
    }

    private Set<CopyFileModel> processSecureSocket(List<BLangRecordLiteral.BLangRecordKeyValueField>
                                                           secureSocketKeyValues) {
        Set<CopyFileModel> copyFileModels = new HashSet<>();
        for (BLangRecordLiteral.BLangRecordKeyValueField keyValue : secureSocketKeyValues) {
            //extract file paths.
            String key = keyValue.getKey().toString();
            if ("keyStore".equals(key)) {
                String keyStoreFile = extractFilePath(keyValue);
                if (keyStoreFile != null) {
                    CopyFileModel copyFileModel = new CopyFileModel();
                    copyFileModel.setSource(keyStoreFile);
                    copyFileModel.setTarget(keyStoreFile);
                    copyFileModels.add(copyFileModel);
                }
            } else if ("trustStore".equals(key)) {
                String trustStoreFile = extractFilePath(keyValue);
                if (trustStoreFile != null) {
                    CopyFileModel copyFileModel = new CopyFileModel();
                    copyFileModel.setSource(trustStoreFile);
                    copyFileModel.setTarget(trustStoreFile);
                    copyFileModels.add(copyFileModel);
                }
            }
        }
        return copyFileModels;

    }

    private String extractFilePath(BLangRecordLiteral.BLangRecordKeyValueField keyValue) {
        List<BLangRecordLiteral.BLangRecordKeyValueField> keyStoreConfigs =
                getKeyValuePairs((BLangRecordLiteral) keyValue.valueExpr);
        for (BLangRecordLiteral.BLangRecordKeyValueField keyStoreConfig : keyStoreConfigs) {
            String configKey = keyStoreConfig.getKey().toString();
            if ("path".equals(configKey)) {
                return keyStoreConfig.getValue().toString();
            }
        }
        return null;
    }

    @Override
    public void codeGenerated(PackageID moduleID, Path executableJarFile) {
        DockerContext.getInstance().setCurrentPackage(moduleID.toString());
        if (DockerContext.getInstance().getDataHolder().isCanProcess()) {
            executableJarFile = executableJarFile.toAbsolutePath();
            if (null != executableJarFile.getParent() && Files.exists(executableJarFile.getParent())) {
                // docker folder location for a single bal file.
                DockerAnnotationProcessor dockerAnnotationProcessor = new DockerAnnotationProcessor();
                Path dockerOutputPath = executableJarFile.getParent().resolve(ARTIFACT_DIRECTORY);
                if (null != executableJarFile.getParent().getParent().getParent() &&
                        Files.exists(executableJarFile.getParent().getParent().getParent())) {
                    // if executable came from a ballerina project
                    Path projectRoot = executableJarFile.getParent().getParent().getParent();
                    if (Files.exists(projectRoot.resolve("Ballerina.toml"))) {
                        dockerOutputPath = projectRoot.resolve("target")
                                .resolve(ARTIFACT_DIRECTORY)
                                .resolve(extractUberJarName(executableJarFile));
                    }
                }

                try {
                    DockerPluginUtils.deleteDirectory(dockerOutputPath);
                    dockerAnnotationProcessor.processDockerModel(DockerContext.getInstance().getDataHolder(),
                            executableJarFile, dockerOutputPath);
                } catch (DockerPluginException e) {
                    String errorMessage = "module [" + moduleID + "] " + e.getMessage();
                    printError(errorMessage);
                    pluginLog.error(errorMessage, e);
                    try {
                        DockerPluginUtils.deleteDirectory(dockerOutputPath);
                    } catch (DockerPluginException ignored) {
                        //ignored
                    }
                }
            } else {
                printError("error in resolving docker generation location.");
                pluginLog.error("error in resolving docker generation location.");
            }
        }
    }

    private enum DockerAnnotation {
        Config,
        CopyFiles,
        Expose
    }
}
