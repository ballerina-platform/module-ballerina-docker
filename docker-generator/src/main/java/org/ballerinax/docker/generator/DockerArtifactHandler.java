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
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.LocalDirectorySSLConfig;
import com.github.dockerjava.core.RemoteApiVersion;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinax.docker.generator.exceptions.DockerGenException;
import org.ballerinax.docker.generator.models.CopyFileModel;
import org.ballerinax.docker.generator.models.DockerModel;
import org.ballerinax.docker.generator.utils.DockerGenUtils;
import org.ballerinax.docker.generator.utils.DockerImageName;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.ballerinax.docker.generator.DockerGenConstants.EXECUTABLE_JAR;
import static org.ballerinax.docker.generator.DockerGenConstants.REGISTRY_SEPARATOR;
import static org.ballerinax.docker.generator.DockerGenConstants.TAG_SEPARATOR;
import static org.ballerinax.docker.generator.utils.DockerGenUtils.cleanErrorMessage;
import static org.ballerinax.docker.generator.utils.DockerGenUtils.copyFileOrDirectory;
import static org.ballerinax.docker.generator.utils.DockerGenUtils.isBlank;
import static org.ballerinax.docker.generator.utils.DockerGenUtils.printDebug;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.FILE_NAME_PERIOD_SEPERATOR;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MODULE_INIT_CLASS_NAME;

/**
 * Generates Docker artifacts from annotations.
 */
public class DockerArtifactHandler {

    private final DockerError dockerBuildError = new DockerError();
    private final DockerModel dockerModel;
    private final DockerClient dockerClient;
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

    private String getModuleLevelClassName(String orgName, String moduleName, String version) {
        String className = MODULE_INIT_CLASS_NAME.replace(".", FILE_NAME_PERIOD_SEPERATOR);
        // handle source file path start with '/'.

        if (!moduleName.equals(".")) {
            if (!version.equals("")) {
                version = version.split("\\.")[0];
                className = cleanupName(version) + "/" + className;
            }
            className = cleanupName(moduleName) + "/" + className;
        }

        if (!orgName.equalsIgnoreCase("$anon")) {
            className = cleanupName(orgName) + "/" + className;
        }

        return "'" + className + "'";
    }

    private String cleanupName(String name) {
        return name.replace(".", "_");
    }

    public void createArtifacts(PrintStream outStream, String logAppender, Path jarFilePath, Path outputDir)
            throws DockerGenException {
        String dockerContent;
        if (!isWindowsBuild()) {
            dockerContent = generateThinJarDockerfile();
        } else {
            dockerContent = generateThinJarWindowsDockerfile();
        }
        copyNativeJars(outputDir);
        try {
            String logStepCount = this.dockerModel.isBuildImage() ? "2" : "1";
            outStream.print(logAppender + " - complete 0/" + logStepCount + " \r");
            DockerGenUtils.writeToFile(dockerContent, outputDir.resolve("Dockerfile"));
            outStream.print(logAppender + " - complete 1/" + logStepCount + " \r");
            Path jarLocation = outputDir.resolve(DockerGenUtils.extractJarName(jarFilePath) + EXECUTABLE_JAR);
            copyFileOrDirectory(jarFilePath, jarLocation);
            for (CopyFileModel copyFileModel : this.dockerModel.getCopyFiles()) {
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
            }
        } catch (IOException e) {
            throw new DockerGenException("unable to write content to " + outputDir);
        }
    }

    private void copyNativeJars(Path outputDir) throws DockerGenException {
        for (Path jarPath : this.dockerModel.getDependencyJarPaths()) {
            // Copy jar files
            Path target = outputDir.resolve(jarPath.getFileName());
            Path sourcePath = jarPath;
            if (!sourcePath.isAbsolute()) {
                sourcePath = sourcePath.toAbsolutePath();
            }
            copyFileOrDirectory(sourcePath, target);
        }
    }

    private DockerClient createClient() {
        DefaultDockerClientConfig.Builder dockerClientConfigBuilder =
                DefaultDockerClientConfig.createDefaultConfigBuilder();

        // if windows, consider DOCKER_HOST as "tcp://localhost:2375"
        if (System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("win")) {
            dockerClientConfigBuilder.withDockerHost("tcp://localhost:2375");
        }

        // set docker host
        if (null != this.dockerModel.getDockerHost()) {
            dockerClientConfigBuilder.withDockerHost(this.dockerModel.getDockerHost());
        }

        // set docker cert path
        if (null != this.dockerModel.getDockerCertPath()) {
            dockerClientConfigBuilder.withDockerCertPath(this.dockerModel.getDockerCertPath());
        }

        // set docker API version
        if (null != this.dockerModel.getDockerAPIVersion()) {
            dockerClientConfigBuilder.withApiVersion(this.dockerModel.getDockerAPIVersion());
        }

        // set docker registry url
        if (null != this.dockerModel.getRegistry()) {
            dockerClientConfigBuilder.withRegistryUrl(this.dockerModel.getRegistry());
        }

        if (null != this.dockerModel.getDockerConfig()) {
            dockerClientConfigBuilder.withDockerConfig(this.dockerModel.getDockerConfig());
        }

        this.dockerClientConfig = dockerClientConfigBuilder.build();
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
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig.getDockerHost())
                .sslConfig(dockerClientConfig.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(60))
                .responseTimeout(Duration.ofSeconds(60))
                .build();

        return DockerClientBuilder.getInstance(dockerClientConfig).withDockerHttpClient(httpClient).build();
    }

    /**
     * Create docker image.
     *
     * @param dockerDir dockerfile directory
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
            String message = ex.getMessage();
            if (message.contains("java.net.SocketException: Connection refused")) {
                this.dockerBuildError.setErrorMsg("unable to connect to docker host: " +
                        this.dockerClientConfig.getDockerHost());
            } else {
                this.dockerBuildError.setErrorMsg("unable to build docker image: " +
                        cleanErrorMessage(message));
            }
        }

        handleError(this.dockerBuildError);
    }


    private void handleError(DockerError dockerError) throws DockerGenException {
        if (dockerError.isError()) {
            throw new DockerGenException(dockerError.getErrorMsg());
        }
    }

    /**
     * Generate Dockerfile content using thin jar.
     *
     * @return Dockerfile content as a string
     */
    private String generateThinJarDockerfile() {
        StringBuilder dockerfileContent = new StringBuilder();
        dockerfileContent.append("# Auto Generated Dockerfile\n");
        dockerfileContent.append("FROM ").append(this.dockerModel.getBaseImage()).append("\n");
        dockerfileContent.append("\n");
        dockerfileContent.append("LABEL maintainer=\"dev@ballerina.io\"").append("\n");
        // Append Jar copy instructions without observability jar and executable jar
        this.dockerModel.getDependencyJarPaths()
                .stream()
                .map(Path::getFileName)
                .filter(path -> !(path.toString().endsWith("-observability-symbols.jar") ||
                        path.toString().endsWith(dockerModel.getJarFileName())))
                .collect(Collectors.toCollection(TreeSet::new))
                .forEach(path -> {
                            dockerfileContent.append("COPY ")
                                    .append(path)
                                    .append(" ").append(getWorkDir())
                                    .append("/jars/ \n");
                            //TODO: Remove once https://github.com/moby/moby/issues/37965 is fixed.
                            boolean isCiBuild = "true".equals(System.getenv().get("CI_BUILD"));
                            if (isCiBuild) {
                                dockerfileContent.append("RUN true \n");
                            }
                        }
                );
        // Append Jar copy for observability jar and executable jar
        this.dockerModel.getDependencyJarPaths().forEach(path -> {
                    if (path.toString().endsWith("observability-symbols.jar") ||
                            path.toString().endsWith(dockerModel.getJarFileName())) {
                        dockerfileContent.append("COPY ")
                                .append(path.getFileName())
                                .append(" ").append(getWorkDir())
                                .append("/jars/ \n");
                    }
                }
        );
        appendUser(dockerfileContent);
        dockerfileContent.append("WORKDIR ").append(getWorkDir()).append("\n");
        appendCommonCommands(dockerfileContent);
        if (isBlank(this.dockerModel.getCmd())) {
            PackageID packageID = this.dockerModel.getPkgId();
            final String mainClass = getModuleLevelClassName(packageID.orgName.value, packageID.name.value,
                    packageID.version.value);
            if (this.dockerModel.isEnableDebug()) {
                dockerfileContent.append("CMD java -Xdiag -agentlib:jdwp=transport=dt_socket,server=y,suspend=n," +
                                "address='*:")
                        .append(this.dockerModel.getDebugPort()).append("' -cp \"")
                        .append(this.dockerModel.getJarFileName()).append(":jars/*\" ").append(mainClass);
            } else {
                dockerfileContent.append("CMD java -Xdiag -cp \"").append(this.dockerModel.getJarFileName())
                        .append(":jars/*\" ").append(mainClass).append(" || cat ballerina-internal.log");
            }
        } else {
            dockerfileContent.append(this.dockerModel.getCmd());
        }
        if (!DockerGenUtils.isBlank(this.dockerModel.getCommandArg())) {
            dockerfileContent.append(this.dockerModel.getCommandArg());
        }
        dockerfileContent.append("\n");

        return dockerfileContent.toString();
    }

    private void appendUser(StringBuilder dockerfileContent) {
        if (this.dockerModel.getBaseImage().equals(DockerGenConstants.OPENJDK_11_JRE_SLIM_BASE)) {
            dockerfileContent.append("RUN addgroup troupe \\").append(System.lineSeparator());
            dockerfileContent.append("    && adduser -S -s /bin/bash -g 'ballerina' -G troupe -D ballerina \\")
                    .append("\n");
            dockerfileContent.append("    && apk add --update --no-cache bash \\").append(System.lineSeparator());
            dockerfileContent.append("    && chown -R ballerina:troupe /usr/bin/java \\")
                    .append(System.lineSeparator());
            dockerfileContent.append("    && rm -rf /var/cache/apk/*").append(System.lineSeparator());
            dockerfileContent.append("\n");
        }
    }

    private String generateThinJarWindowsDockerfile() {
        final String separator = "\\";
        StringBuilder dockerfileContent = new StringBuilder();
        dockerfileContent.append("# Auto Generated Dockerfile\n");
        dockerfileContent.append("FROM ").append(this.dockerModel.getBaseImage()).append("\n");
        dockerfileContent.append(System.lineSeparator());
        dockerfileContent.append("LABEL maintainer=\"dev@ballerina.io\"").append(System.lineSeparator());
        dockerfileContent.append(System.lineSeparator());
        dockerfileContent.append("WORKDIR ").append(getWorkDir()).append(System.lineSeparator());

        for (Path path : this.dockerModel.getDependencyJarPaths()) {
            dockerfileContent.append("COPY ").append(path.getFileName()).append(getWorkDir())
                    .append("jars").append(separator);
            dockerfileContent.append(System.lineSeparator());
        }
        dockerfileContent.append(System.lineSeparator());
        appendCommonCommands(dockerfileContent);
        if (isBlank(this.dockerModel.getCmd())) {
            PackageID packageID = this.dockerModel.getPkgId();
            final String mainClass = getModuleLevelClassName(packageID.orgName.value, packageID.name.value,
                    packageID.version.value);
            if (this.dockerModel.isEnableDebug()) {
                dockerfileContent.append("CMD java -Xdiag -agentlib:jdwp=transport=dt_socket,server=y,suspend=n," +
                        "address='*:").append(this.dockerModel.getDebugPort()).append("' -cp \"")
                        .append(this.dockerModel.getJarFileName()).append(":jars/*\" ").append(mainClass);
            } else {
                dockerfileContent.append("CMD java -Xdiag -cp \"").append(this.dockerModel.getJarFileName())
                        .append(":jars/*\" ").append(mainClass);
            }
        } else {
            dockerfileContent.append(this.dockerModel.getCmd());
        }
        dockerfileContent.append(System.lineSeparator());
        if (!DockerGenUtils.isBlank(this.dockerModel.getCommandArg())) {
            dockerfileContent.append(this.dockerModel.getCommandArg());
        }
        dockerfileContent.append(System.lineSeparator());

        return dockerfileContent.toString();
    }

    private void appendCommonCommands(StringBuilder dockerfileContent) {
        dockerfileContent.append("COPY ").append(this.dockerModel.getJarFileName()).append(" ").append(getWorkDir())
                .append(System.lineSeparator());
        this.dockerModel.getEnv().forEach((key, value) -> dockerfileContent.append("ENV ").
                append(key).append("=").append(value).append(System.lineSeparator()));

        this.dockerModel.getCopyFiles().forEach(file -> {
            // Extract the source filename relative to docker folder.
            String sourceFileName = String.valueOf(Paths.get(file.getSource()).getFileName());
            dockerfileContent.append("COPY ")
                    .append(sourceFileName)
                    .append(" ")
                    .append(file.getTarget())
                    .append(System.lineSeparator());
        });

        dockerfileContent.append(System.lineSeparator());

        if (this.dockerModel.isService() && this.dockerModel.getPorts().size() > 0) {
            dockerfileContent.append("EXPOSE ");
            this.dockerModel.getPorts().forEach(port -> dockerfileContent.append(" ").append(port));
        }
        dockerfileContent.append(System.lineSeparator());
        if (this.dockerModel.getBaseImage().equals(DockerGenConstants.OPENJDK_11_JRE_SLIM_BASE)) {
            dockerfileContent.append("USER ballerina").append("\n");
            dockerfileContent.append(System.lineSeparator());
        }
    }

    private boolean isWindowsBuild() {
        return Boolean.parseBoolean(System.getenv(DockerGenConstants.ENABLE_WINDOWS_BUILD));
    }

    private String getWorkDir() {
        return isWindowsBuild() ? "C:\\ballerina\\home\\" : "/home/ballerina";
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

        @Override
        public void onError(Throwable throwable) {
            printDebug(throwable.getMessage());
            try {
                super.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
