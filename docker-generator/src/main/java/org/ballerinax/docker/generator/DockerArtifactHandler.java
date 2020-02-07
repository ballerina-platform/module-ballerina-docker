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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.LocalDirectorySSLConfig;
import com.github.dockerjava.core.RemoteApiVersion;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import org.apache.commons.io.FilenameUtils;
import org.ballerinax.docker.generator.exceptions.DockerGenException;
import org.ballerinax.docker.generator.models.CopyFileModel;
import org.ballerinax.docker.generator.models.DockerModel;
import org.ballerinax.docker.generator.utils.DockerGenUtils;
import org.ballerinax.docker.generator.utils.DockerImageName;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.ballerinax.docker.generator.DockerGenConstants.EXECUTABLE_JAR;
import static org.ballerinax.docker.generator.DockerGenConstants.REGISTRY_SEPARATOR;
import static org.ballerinax.docker.generator.DockerGenConstants.TAG_SEPARATOR;
import static org.ballerinax.docker.generator.utils.DockerGenUtils.cleanErrorMessage;
import static org.ballerinax.docker.generator.utils.DockerGenUtils.copyFileOrDirectory;
import static org.ballerinax.docker.generator.utils.DockerGenUtils.isBlank;
import static org.ballerinax.docker.generator.utils.DockerGenUtils.printDebug;

/**
 * Generates Docker artifacts from annotations.
 */
public class DockerArtifactHandler {
    
    private static final boolean WINDOWS_BUILD =
            Boolean.parseBoolean(System.getenv(DockerGenConstants.ENABLE_WINDOWS_BUILD));
    private final DockerError dockerBuildError = new DockerError();
    private final DockerError dockerPushError = new DockerError();
    private DockerModel dockerModel;
    private DockerClient dockerClient;
    private DefaultDockerClientConfig dockerClientConfig;
    
    public DockerArtifactHandler(DockerModel dockerModel) {
        String registry = dockerModel.getRegistry();
        String imageName = dockerModel.getName();
        imageName = !isBlank(registry) ? registry + REGISTRY_SEPARATOR + imageName + TAG_SEPARATOR
                + dockerModel.getTag() :
                imageName + TAG_SEPARATOR + dockerModel.getTag();
        dockerModel.setName(imageName);

        this.dockerModel = dockerModel;
        this.dockerClient = createClient();
    }

    public void createArtifacts(PrintStream outStream, String logAppender, Path uberJarFilePath, Path outputDir)
            throws DockerGenException {
        String dockerContent;
        if (!WINDOWS_BUILD) {
            dockerContent = generateDockerfile();
        } else {
            dockerContent = generateDockerfileForWindows();
        }
        try {
            String logStepCount = dockerModel.isBuildImage() ? (dockerModel.isPush() ? "3" : "2") : "1";
            outStream.print(logAppender + " - complete 0/" + logStepCount + " \r");
            DockerGenUtils.writeToFile(dockerContent, outputDir.resolve("Dockerfile"));
            outStream.print(logAppender + " - complete 1/" + logStepCount + " \r");
            Path uberJarLocation = outputDir.resolve(DockerGenUtils.extractUberJarName(uberJarFilePath) +
                    EXECUTABLE_JAR);
            copyFileOrDirectory(uberJarFilePath, uberJarLocation);
            for (CopyFileModel copyFileModel : dockerModel.getCopyFiles()) {
                // Copy external files to docker folder
                Path target = outputDir.resolve(Paths.get(copyFileModel.getSource()).getFileName());
                Path sourcePath = Paths.get(copyFileModel.getSource());
                if (!sourcePath.isAbsolute()) {
                    sourcePath = sourcePath.toAbsolutePath();
                }
                copyFileOrDirectory(sourcePath, target);

            }
            //check image build is enabled.
            if (this.dockerModel.isBuildImage()) {
                buildImage(outputDir);
                outStream.print(logAppender + " - complete 2/" + logStepCount + " \r");
                Files.delete(uberJarLocation);
                //push only if image push is enabled.
                if (this.dockerModel.isPush()) {
                    pushImage();
                    outStream.print(logAppender + " - complete 3/" + logStepCount + " \r");
                }
            }
        } catch (IOException e) {
            throw new DockerGenException("unable to write content to " + outputDir);
        } catch (InterruptedException e) {
            throw new DockerGenException("unable to create Docker images " + e.getMessage());
        }
    }
    
    private DockerClient createClient() {
        DefaultDockerClientConfig.Builder dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder();
        // set docker host
        if (null != this.dockerModel.getDockerHost()) {
            dockerClientConfig.withDockerHost(this.dockerModel.getDockerHost());
        }
        
        // set docker cert path
        if (null != this.dockerModel.getDockerCertPath()) {
            dockerClientConfig.withDockerCertPath(this.dockerModel.getDockerCertPath());
        }
    
        // set docker API version
        if (null != this.dockerModel.getDockerAPIVersion()) {
            dockerClientConfig.withApiVersion(this.dockerModel.getDockerAPIVersion());
        }
    
        // set docker registry url
        if (null != this.dockerModel.getRegistry()) {
            dockerClientConfig.withRegistryUrl(this.dockerModel.getRegistry());
        }
    
        // set docker registry username
        if (null != this.dockerModel.getUsername()) {
            dockerClientConfig.withRegistryUsername(this.dockerModel.getUsername());
        }
    
        // set docker registry password
        if (null != this.dockerModel.getPassword()) {
            dockerClientConfig.withRegistryPassword(this.dockerModel.getPassword());
        }
    
        this.dockerClientConfig = dockerClientConfig.build();
        printDebug("docker client host: " + this.dockerClientConfig.getDockerHost());
        
        if (!this.dockerClientConfig.getApiVersion().equals(RemoteApiVersion.unknown())) {
            printDebug("docker client API version: " + this.dockerClientConfig.getApiVersion().getVersion());
        } else {
            printDebug("docker client API version: not-set");
        }
        
        if (null != this.dockerClientConfig.getSSLConfig() &&
            this.dockerClientConfig.getSSLConfig() instanceof LocalDirectorySSLConfig) {
            LocalDirectorySSLConfig sslConfig = (LocalDirectorySSLConfig) this.dockerClientConfig.getSSLConfig();
            printDebug("docker client certs path: " + sslConfig.getDockerCertPath());
            printDebug("docker client TLS verify: true");
        } else {
            printDebug("docker client TLS verify: false");
        }
        
        return DockerClientBuilder.getInstance(dockerClientConfig).build();
    }

    /**
     * Create docker image.
     *
     * @param dockerDir   dockerfile directory
     */
    public void buildImage(Path dockerDir) throws DockerGenException {
        // validate docker image name
        DockerImageName.validate(this.dockerModel.getName());

        printDebug("building docker image `" + this.dockerModel.getName() + "` from directory `" + dockerDir + "`.");
    
        try {
            this.dockerClient.buildImageCmd(dockerDir.toFile())
                .withNoCache(true)
                .withForcerm(true)
                .withTags(Collections.singleton(this.dockerModel.getName()))
                .exec(new DockerBuildImageCallback())
                .awaitImageId();
        } catch (RuntimeException ex) {
            if (ex.getMessage().contains("java.net.SocketException: Connection refused")) {
                this.dockerBuildError.setErrorMsg("unable to connect to docker host: " +
                                                  this.dockerClientConfig.getDockerHost());
            } else {
                this.dockerBuildError.setErrorMsg("unable to build docker image: " +
                                                  cleanErrorMessage(ex.getMessage()));
            }
        }
    
        handleError(this.dockerBuildError);
    }
    
    /**
     * Push docker image.
     *
     * @throws InterruptedException When error with docker build process
     */
    public void pushImage() throws InterruptedException, DockerGenException {
        printDebug("pushing docker image `" + this.dockerModel.getName() + "`.");
        this.dockerClient.pushImageCmd(this.dockerModel.getName())
                .exec(new DockerImagePushCallback()).
                awaitCompletion();
        handleError(this.dockerPushError);
    }

    private void handleError(DockerError dockerError) throws DockerGenException {
        if (dockerError.isError()) {
            throw new DockerGenException(dockerError.getErrorMsg());
        }
    }
    
    /**
     * Generate Dockerfile content.
     *
     * @return Dockerfile content as a string
     */
    private String generateDockerfile() {
        StringBuilder dockerfileContent = new StringBuilder();
        dockerfileContent.append("# Auto Generated Dockerfile\n");
        dockerfileContent.append("FROM ").append(this.dockerModel.getBaseImage()).append("\n");
        dockerfileContent.append("\n");
        dockerfileContent.append("LABEL maintainer=\"dev@ballerina.io\"").append("\n");
        dockerfileContent.append("\n");

        if (this.dockerModel.getBaseImage().equals(DockerGenConstants.OPENJDK_8_JRE_ALPINE_BASE_IMAGE)) {
            dockerfileContent.append("RUN addgroup troupe \\").append("\n");
            dockerfileContent.append("    && adduser -S -s /bin/bash -g 'ballerina' -G troupe -D ballerina \\")
                    .append("\n");
            dockerfileContent.append("    && apk add --update --no-cache bash \\").append("\n");
            dockerfileContent.append("    && chown -R ballerina:troupe /usr/bin/java \\").append("\n");
            dockerfileContent.append("    && rm -rf /var/cache/apk/*").append("\n");
            dockerfileContent.append("\n");
        }

        dockerfileContent.append("WORKDIR /home/ballerina").append("\n");
        dockerfileContent.append("\n");
    
        dockerfileContent.append("COPY ").append(this.dockerModel.getUberJarFileName()).append(" /home/ballerina")
                .append("\n");
    
        this.dockerModel.getCopyFiles().forEach(file -> {
            // Extract the source filename relative to docker folder.
            String sourceFileName = String.valueOf(Paths.get(file.getSource()).getFileName());
            dockerfileContent.append("COPY ")
                    .append(sourceFileName)
                    .append(" ")
                    .append(file.getTarget())
                    .append("\n");
        });

        dockerfileContent.append("\n");
        
        if (this.dockerModel.isService() && this.dockerModel.getPorts().size() > 0) {
            dockerfileContent.append("EXPOSE ");
            this.dockerModel.getPorts().forEach(port -> dockerfileContent.append(" ").append(port));
        }
        dockerfileContent.append("\n");
        if (this.dockerModel.getBaseImage().equals(DockerGenConstants.OPENJDK_8_JRE_ALPINE_BASE_IMAGE)) {
            dockerfileContent.append("USER ballerina").append("\n");
            dockerfileContent.append("\n");
        }
        
        return appendCMD(dockerfileContent);
    }

    private String generateDockerfileForWindows() {
        String dockerBase = "# Auto Generated Dockerfile\n" +
                "\n" +
                "FROM " + this.dockerModel.getBaseImage() + "\n" +
                "LABEL maintainer=\"dev@ballerina.io\"\n" +
                "\n" +
                "COPY " + this.dockerModel.getUberJarFileName() + " C:\\\\ballerina\\\\home \n\n";

        StringBuilder stringBuilder = new StringBuilder(dockerBase);
        this.dockerModel.getCopyFiles().forEach(file -> {
            // Extract the source filename relative to docker folder.
            String sourceFileName = String.valueOf(Paths.get(file.getSource()).getFileName());
            stringBuilder.append("COPY ")
                    .append(FilenameUtils.separatorsToWindows(sourceFileName))
                    .append(" ")
                    .append(FilenameUtils.separatorsToWindows(file.getTarget()))
                    .append("\n");
        });

        if (this.dockerModel.isService() && this.dockerModel.getPorts().size() > 0) {
            stringBuilder.append("EXPOSE ");
            this.dockerModel.getPorts().forEach(port -> stringBuilder.append(" ").append(port));
        }

        stringBuilder.append("\n");

        return appendCMD(stringBuilder);
    }

    private String appendCMD(StringBuilder stringBuilder) {

        if (null == this.dockerModel.getCmd() || "".equals(this.dockerModel.getCmd())) {
            if (this.dockerModel.isEnableDebug()) {
                stringBuilder.append("CMD java -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket," +
                        "server=y,suspend=y,address=").append(dockerModel.getDebugPort())
                        .append(" -jar ").append(dockerModel.getUberJarFileName());
            } else {
                stringBuilder.append("CMD java -jar ").append(dockerModel.getUberJarFileName());
            }
            if (!DockerGenUtils.isBlank(dockerModel.getCommandArg())) {
                stringBuilder.append(dockerModel.getCommandArg());
            }
        } else {
            stringBuilder.append(this.dockerModel.getCmd());
        }

        stringBuilder.append("\n");

        return stringBuilder.toString();
    }

    /**
     * Class to hold docker errors.
     */
    private static class DockerError {
        private boolean error;
        private String errorMsg;

        DockerError() {
            this.error = false;
        }

        boolean isError() {
            return error;
        }

        String getErrorMsg() {
            return errorMsg;
        }

        void setErrorMsg(String errorMsg) {
            this.error = true;
            this.errorMsg = errorMsg;
        }
    }
    
    private class DockerBuildImageCallback extends BuildImageResultCallback {
        @Override
        public void onNext(BuildResponseItem item) {
            // handling error
            if (item.isErrorIndicated()) {
                StringBuilder errString = new StringBuilder("[build][error]: ");
                ResponseItem.ErrorDetail errDetail = item.getErrorDetail();
                if (null != errDetail && null != errDetail.getCode()) {
                    errString.append("(").append(errDetail.getCode()).append(") ");
                }
            
                if (null != errDetail && null != errDetail.getMessage()) {
                    errString.append(errDetail.getMessage());
                }
            
                String errorMessage = errString.toString();
                printDebug(errorMessage);
                dockerBuildError.setErrorMsg("unable to build docker image: " + errorMessage);
            }
    
            String streamLog = item.getStream();
            if (null != streamLog && !"".equals(streamLog.replaceAll("\n", ""))) {
                printDebug("[build][stream] " + streamLog.replaceAll("\n", ""));
            }
    
            String statusLog = item.getStatus();
            if (null != statusLog && !"".equals(statusLog.replaceAll("\n", ""))) {
                printDebug("[build][status] " + statusLog.replaceAll("\n", ""));
            }
    
            String imageIdLog = item.getImageId();
            if (null != imageIdLog) {
                printDebug("[build][image-id]: " + imageIdLog);
            }
        
            super.onNext(item);
        }
    }
    
    private class DockerImagePushCallback extends PushImageResultCallback {
        @Override
        public void onNext(PushResponseItem item) {
            // handling error
            if (item.isErrorIndicated()) {
                StringBuilder errString = new StringBuilder("[push][error]: ");
    
                ResponseItem.ErrorDetail errDetail = null;
                if (null != item.getErrorDetail()) {
                    errDetail = item.getErrorDetail();
                }
                if (null != errDetail && null != errDetail.getCode()) {
                    errString.append("(").append(errDetail.getCode()).append(") ");
                }
            
                if (null != errDetail && null != errDetail.getMessage()) {
                    errString.append(errDetail.getMessage());
                }
            
                String errorMessage = errString.toString();
                printDebug(errorMessage);
                dockerPushError.setErrorMsg("unable to push docker image: " + errorMessage);
            }
    
            String streamLog = item.getStream();
            if (null != streamLog && !"".equals(streamLog.replaceAll("\n", ""))) {
                printDebug("[push][stream] " + streamLog.replaceAll("\n", ""));
            }
            
            String statusLog = item.getStatus();
            if (null != statusLog && !"".equals(statusLog.replaceAll("\n", ""))) {
                printDebug("[push][status] " + statusLog.replaceAll("\n", ""));
            }
    
            String idLog = item.getId();
            if (null != idLog) {
                printDebug("[push][ID]: " + idLog);
            }
        
            super.onNext(item);
        }
    }
}
