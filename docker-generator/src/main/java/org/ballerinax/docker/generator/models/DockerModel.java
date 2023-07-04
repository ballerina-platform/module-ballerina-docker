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

package org.ballerinax.docker.generator.models;

import lombok.Getter;
import lombok.Setter;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinax.docker.generator.DockerGenConstants;
import org.ballerinax.docker.generator.exceptions.DockerGenException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.ballerinax.docker.generator.DockerGenConstants.DOCKER_API_VERSION;


/**
 * Docker annotations model class.
 */
@Getter
@Setter
public class DockerModel {
    private final boolean windowsBuild =
            Boolean.parseBoolean(System.getenv(DockerGenConstants.ENABLE_WINDOWS_BUILD));
    private String name;
    private String registry;
    private String tag;
    private boolean buildImage;
    private String baseImage;
    private Set<Integer> ports;
    private boolean enableDebug;
    private int debugPort;
    private String dockerAPIVersion;
    private String dockerHost;
    private String dockerCertPath;
    private boolean isService;
    private String jarFileName;
    private Set<CopyFileModel> externalFiles;
    private String commandArg;
    private String cmd;
    private Map<String, String> env;
    private String dockerConfig;
    private Set<Path> dependencyJarPaths;
    private PackageID pkgId;

    public DockerModel() {
        // Initialize with default values except for image name
        this.tag = "latest";
        this.buildImage = true;
        this.baseImage = windowsBuild ? DockerGenConstants.OPENJDK_17_JRE_WINDOWS_BASE_IMAGE :
                DockerGenConstants.OPENJDK_17_JRE_SLIM_BASE;
        this.enableDebug = false;
        this.debugPort = 5005;
        this.setDockerAPIVersion(System.getenv(DOCKER_API_VERSION));
        externalFiles = new HashSet<>();
        commandArg = "";
        env = new HashMap<>();
        dependencyJarPaths = new TreeSet<>();
    }

    public void setDockerAPIVersion(String dockerAPIVersion) {
        if (null != dockerAPIVersion && !dockerAPIVersion.startsWith("v")) {
            dockerAPIVersion = "v" + dockerAPIVersion;
        }

        this.dockerAPIVersion = dockerAPIVersion;
    }

    public void addDependencyJarPaths(Set<Path> paths) {
        this.dependencyJarPaths.addAll(paths);
    }

    public Set<Path> getDependencyJarPaths() {
        return this.dependencyJarPaths.stream().sorted().collect(Collectors.toSet());
    }

    public Set<CopyFileModel> getCopyFiles() {
        return externalFiles;
    }

    public void setCopyFiles(Set<CopyFileModel> externalFiles) throws DockerGenException {
        this.externalFiles = externalFiles;
        for (CopyFileModel externalFile : externalFiles) {
            if (!externalFile.isBallerinaConf()) {
                continue;
            }

            if (Files.isDirectory(Paths.get(externalFile.getSource()))) {
                throw new DockerGenException("invalid config file given: " + externalFile.getSource());
            }
            this.env.put("BALCONFIGFILE", externalFile.getTarget());
        }
    }

    public void addCommandArg(String commandArg) {
        this.commandArg += commandArg;
    }

    public String getCmd() {
        if (this.cmd == null) {
            return null;
        }

        String configFile = "";
        for (CopyFileModel externalFile : externalFiles) {
            if (!externalFile.isBallerinaConf()) {
                continue;
            }
            configFile = externalFile.getTarget();
        }

        return this.cmd
                .replace("${APP}", this.jarFileName)
                .replace("${CONFIG_FILE}", configFile);
    }

}
