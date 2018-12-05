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

package org.ballerinax.docker.generator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.docker.api.model.AuthConfig;
import io.fabric8.docker.api.model.AuthConfigBuilder;
import io.fabric8.docker.client.Config;
import io.fabric8.docker.client.ConfigBuilder;
import io.fabric8.docker.client.DefaultDockerClient;
import io.fabric8.docker.client.DockerClient;
import io.fabric8.docker.client.utils.RegistryUtils;
import io.fabric8.docker.dsl.EventListener;
import io.fabric8.docker.dsl.OutputHandle;
import org.ballerinax.docker.generator.exceptions.DockerGenException;
import org.ballerinax.docker.generator.models.CopyFileModel;
import org.ballerinax.docker.generator.models.DockerModel;
import org.ballerinax.docker.generator.utils.DockerGenUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import static org.ballerinax.docker.generator.DockerGenConstants.BALX;
import static org.ballerinax.docker.generator.utils.DockerGenUtils.copyFileOrDirectory;


/**
 * Generates Docker artifacts from annotations.
 */
public class DockerArtifactHandler {

    private final CountDownLatch pushDone = new CountDownLatch(1);
    private final CountDownLatch buildDone = new CountDownLatch(1);
    private DockerModel dockerModel;

    public DockerArtifactHandler(DockerModel dockerModel) {
        this.dockerModel = dockerModel;
        if (!DockerGenUtils.isBlank(dockerModel.getDockerCertPath())) {
            System.setProperty("docker.cert.path", dockerModel.getDockerCertPath());
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
                buildImage(outputDir);
                outStream.print(logAppender + " - complete 2/3 \r");
                Files.delete(Paths.get(balxDestination));
                //push only if image build is enabled.
                if (dockerModel.isPush()) {
                    pushImage();
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

    private static void disableFailOnUnknownProperties() {
        // Disable fail on unknown properties using reflection to avoid docker client issue.
        // (https://github.com/fabric8io/docker-client/issues/106).
        final Field jsonMapperField;
        try {
            jsonMapperField = Config.class.getDeclaredField("JSON_MAPPER");
            assert jsonMapperField != null;
            jsonMapperField.setAccessible(true);
            final ObjectMapper objectMapper = (ObjectMapper) jsonMapperField.get(null);
            assert objectMapper != null;
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }

    /**
     * Create docker image.
     *
     * @param dockerDir   dockerfile directory
     * @throws InterruptedException When error with docker build process
     * @throws IOException          When error with docker build process
     */
    private void buildImage(String dockerDir) throws InterruptedException, IOException, DockerGenException {
        disableFailOnUnknownProperties();
        Config dockerClientConfig = new ConfigBuilder()
                .withDockerUrl(dockerModel.getDockerHost())
                .build();
        DockerClient client = new io.fabric8.docker.client.DefaultDockerClient(dockerClientConfig);
        final DockerError dockerError = new DockerError();
        OutputHandle buildHandle = client.image()
                .build()
                .withRepositoryName(dockerModel.getName())
                .withNoCache()
                .alwaysRemovingIntermediate()
                .usingListener(new EventListener() {
                    @Override
                    public void onSuccess(String message) {
                        buildDone.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        dockerError.setErrorMsg("Unable to build Docker image: " + message);
                        buildDone.countDown();
                    }

                    @Override
                    public void onError(Throwable t) {
                        dockerError.setErrorMsg("Unable to build Docker image: " + t.getMessage());
                        buildDone.countDown();
                    }

                    @Override
                    public void onEvent(String event) {
                        DockerGenUtils.printDebug(event);
                    }
                })
                .fromFolder(dockerDir);
        buildDone.await();
        buildHandle.close();
        client.close();
        handleError(dockerError);
    }

    private void handleError(DockerError dockerError) throws DockerGenException {
        if (dockerError.isError()) {
            throw new DockerGenException(dockerError.getErrorMsg());
        }
    }

    /**
     * Push docker image.
     *
     * @throws InterruptedException When error with docker build process
     * @throws IOException          When error with docker build process
     */
    private void pushImage() throws InterruptedException, IOException, DockerGenException {
        disableFailOnUnknownProperties();
        AuthConfig authConfig = new AuthConfigBuilder().withUsername(dockerModel.getUsername()).withPassword
                (dockerModel.getPassword())
                .build();
        Config config = new ConfigBuilder()
                .withDockerUrl(dockerModel.getDockerHost())
                .addToAuthConfigs(RegistryUtils.extractRegistry(dockerModel.getName()), authConfig)
                .build();

        DockerClient client = new DefaultDockerClient(config);
        final DockerError dockerError = new DockerError();
        OutputHandle handle = client.image().withName(dockerModel.getName()).push()
                .usingListener(new EventListener() {
                    @Override
                    public void onSuccess(String message) {
                        pushDone.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        pushDone.countDown();
                        dockerError.setErrorMsg("Unable to push Docker image: " + message);
                    }

                    @Override
                    public void onError(Throwable t) {
                        pushDone.countDown();
                        dockerError.setErrorMsg("Unable to push Docker image: " + t.getMessage());
                    }

                    @Override
                    public void onEvent(String event) {
                        DockerGenUtils.printDebug(event);
                    }
                })
                .toRegistry();

        pushDone.await();
        handle.close();
        client.close();
        handleError(dockerError);
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
}
