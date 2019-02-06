/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinax.docker.generator;

import com.google.common.base.Optional;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerCertificatesStore;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.RegistryAuth;
import org.ballerinax.docker.generator.exceptions.DockerGenException;
import org.ballerinax.docker.generator.models.CopyFileModel;
import org.ballerinax.docker.generator.models.DockerModel;
import org.ballerinax.docker.generator.utils.DockerGenUtils;
import org.glassfish.jersey.internal.RuntimeDelegateImpl;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.ext.RuntimeDelegate;

import static org.ballerinax.docker.generator.DockerGenConstants.BALX;
import static org.ballerinax.docker.generator.utils.DockerGenUtils.cleanErrorMessage;
import static org.ballerinax.docker.generator.utils.DockerGenUtils.copyFileOrDirectory;
import static org.ballerinax.docker.generator.utils.DockerGenUtils.printDebug;

/**
 * Generates Docker artifacts from annotations.
 */
public class DockerArtifactHandler {
    private final CountDownLatch pushDone = new CountDownLatch(1);
    private final CountDownLatch buildDone = new CountDownLatch(1);
    private DockerModel dockerModel;
    private DockerCertificatesStore certs;
    
    public DockerArtifactHandler(DockerModel dockerModel) throws DockerGenException {
        try {
            RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
            this.dockerModel = dockerModel;
            if (!DockerGenUtils.isBlank(dockerModel.getDockerCertPath())) {
                Optional<DockerCertificatesStore> certsOptional = DockerCertificates.builder()
                        .dockerCertPath(Paths.get(dockerModel.getDockerCertPath()))
                        .build();
                if (certsOptional.isPresent()) {
                    certs = certsOptional.get();
                }
            }
        } catch (DockerCertificateException e) {
            throw new DockerGenException("Unable to create Docker images " + e.getMessage());
        }
    }
    
    public void createArtifacts(PrintStream outStream, String logAppender, String balxFilePath, String outputDir)
            throws DockerGenException {
        String dockerContent = generateDockerfile();
        try {
            outStream.print(logAppender + " - complete 0/3 \r");
            DockerGenUtils.writeToFile(dockerContent, outputDir + File.separator + "Dockerfile");
            outStream.print(logAppender + " - complete 1/3 \r");
            String balxDestination = outputDir + File.separator + DockerGenUtils.extractBalxName(balxFilePath) + BALX;
            copyFileOrDirectory(balxFilePath, balxDestination);
            for (CopyFileModel copyFileModel : dockerModel.getCopyFiles()) {
                // Copy external files to docker folder
                String target = outputDir + File.separator + Paths.get(copyFileModel.getSource()).getFileName();
                Path sourcePath = Paths.get(copyFileModel.getSource());
                if (!sourcePath.isAbsolute()) {
                    sourcePath = sourcePath.toAbsolutePath();
                }
                copyFileOrDirectory(sourcePath.toString(), target);
                
            }
            //check image build is enabled.
            if (dockerModel.isBuildImage()) {
                buildImage(dockerModel, outputDir);
                outStream.print(logAppender + " - complete 2/3 \r");
                Files.delete(Paths.get(balxDestination));
                //push only if image build is enabled.
                if (dockerModel.isPush()) {
                    pushImage(dockerModel);
                }
                outStream.print(logAppender + " - complete 3/3 \r");
            }
            outStream.print(logAppender + " - complete 3/3 \r");
        } catch (IOException e) {
            throw new DockerGenException("Unable to write content to " + outputDir);
        } catch (InterruptedException e) {
            throw new DockerGenException("Unable to create Docker images " + e.getMessage());
        }
    }
    
    /**
     * Create docker image.
     *
     * @param dockerModel dockerModel object
     * @param dockerDir   dockerfile directory
     * @throws InterruptedException When error with docker build process
     * @throws IOException          When error with docker build process
     */
    public void buildImage(DockerModel dockerModel, String dockerDir) throws
            InterruptedException, IOException, DockerGenException {
        final AtomicReference<String> dockerError = new AtomicReference<>();
        final AtomicReference<String> imageID = new AtomicReference<>();
        String buildID = null;
        DockerClient client = null;
        try {
            client = DefaultDockerClient.builder()
                    .uri(dockerModel.getDockerHost())
                    .dockerCertificates(certs)
                    .build();
    
    
            buildID = client.build(Paths.get(dockerDir), dockerModel.getName(), message -> {
                String buildImageId = message.buildImageId();
                String error = message.error();
                
                if (null != message.progress()) {
                    printDebug(message.progress());
                }
    
                // when an image is built successfully.
                if (null != buildImageId) {
                    printDebug("Image ID: " + buildImageId);
                    imageID.set(buildImageId);
                    buildDone.countDown();
                }
                
                // when there is an error.
                if (null != error) {
                    printDebug("Error message: " + error);
                    dockerError.set("Unable to build Docker image: " + error);
                    buildDone.countDown();
                }
            }, DockerClient.BuildParam.noCache(), DockerClient.BuildParam.forceRm());
            printDebug("Build ID: " + buildID);
        } catch (DockerException e) {
            dockerError.set("Unable to connect to server: " + cleanErrorMessage(e.getMessage()));
            buildDone.countDown();
        }
        buildDone.await();
        
        if (null != dockerError.get()) {
            try {
                if (null != buildID) {
                    client.removeImage(buildID, true, false);
                }
                if (null != imageID.get()) {
                    client.removeImage(imageID.get(), true, false);
                }
            } catch (DockerException e) {
                // ignore
            }
            throw new DockerGenException(dockerError.get());
        }
    }
    
    /**
     * Push docker image.
     *
     * @param dockerModel DockerModel
     * @throws InterruptedException When error with docker build process
     */
    public void pushImage(DockerModel dockerModel) throws InterruptedException, DockerGenException {
        final AtomicReference<String> dockerError = new AtomicReference<>();
    
        RegistryAuth auth = RegistryAuth.builder()
                .username(dockerModel.getUsername())
                .password(dockerModel.getPassword())
                .build();
        
        try {
            DockerClient client;
            client = DefaultDockerClient.builder()
                    .uri(dockerModel.getDockerHost())
                    .dockerCertificates(certs)
                    .build();
            
            client.push(dockerModel.getName(), message -> {
                String digest = message.digest();
                String error = message.error();
    
                if (null != message.progress()) {
                    printDebug(message.progress());
                }
                
                // When image is successfully built.
                if (null != digest) {
                    printDebug("Digest: " + digest);
                    pushDone.countDown();
                }
                
                // When error occurs.
                if (null != error) {
                    printDebug("Error message: " + error);
                    dockerError.set("Unable to push Docker image: " + error);
                    pushDone.countDown();
                }
            }, auth);
        } catch (DockerException e) {
            dockerError.set("Unable to connect to server: " + cleanErrorMessage(e.getMessage()));
            pushDone.countDown();
        }
        pushDone.await();
        if (null != dockerError.get()) {
            throw new DockerGenException(dockerError.get());
        }
    }
    
    /**
     * Generate Dockerfile content.
     *
     * @return Dockerfile content as a string
     */
    private String generateDockerfile() {
        String dockerBase = "# Auto Generated Dockerfile\n" +
                "\n" +
                "FROM " + dockerModel.getBaseImage() + "\n" +
                "LABEL maintainer=\"dev@ballerina.io\"\n" +
                "\n" +
                "COPY " + dockerModel.getBalxFileName() + " /home/ballerina \n\n";

        StringBuilder stringBuilder = new StringBuilder(dockerBase);
        dockerModel.getCopyFiles().forEach(file -> {
            // Extract the source filename relative to docker folder.
            String sourceFileName = String.valueOf(Paths.get(file.getSource()).getFileName());
            stringBuilder.append("COPY ")
                    .append(sourceFileName)
                    .append(" ")
                    .append(file.getTarget())
                    .append("\n");
        });
        
        if (dockerModel.isService() && dockerModel.getPorts().size() > 0) {
            stringBuilder.append("EXPOSE ");
            dockerModel.getPorts().forEach(port -> stringBuilder.append(" ").append(port));
        }
        
        stringBuilder.append("\nCMD ballerina run ");
        
        if (!DockerGenUtils.isBlank(dockerModel.getCommandArg())) {
            stringBuilder.append(dockerModel.getCommandArg());
        }
        
        if (dockerModel.isEnableDebug()) {
            stringBuilder.append(" --debug ").append(dockerModel.getDebugPort());
        }
        stringBuilder.append(" ").append(dockerModel.getBalxFileName());
        stringBuilder.append("\n");
        
        return stringBuilder.toString();
    }
}
