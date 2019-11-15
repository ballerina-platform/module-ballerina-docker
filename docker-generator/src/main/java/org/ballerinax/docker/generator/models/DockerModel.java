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

import com.spotify.docker.client.DockerHost;
import org.ballerinax.docker.generator.DockerGenConstants;
import org.ballerinax.docker.generator.exceptions.DockerGenException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.ballerinax.docker.generator.DockerGenConstants.DOCKER_API_VERSION;


/**
 * Docker annotations model class.
 */
public class DockerModel {
    private static final boolean WINDOWS_BUILD = "true".equals(System.getenv(DockerGenConstants.ENABLE_WINDOWS_BUILD));
    
    private String name;
    private String registry;
    private String tag;
    private boolean push;
    private String username;
    private String password;
    private boolean buildImage;
    private String baseImage;
    private Set<Integer> ports;
    private boolean enableDebug;
    private int debugPort;
    private String dockerAPIVersion;
    private String dockerHost;
    private String dockerCertPath;
    private boolean isService;
    private String uberJarFileName;
    private Set<CopyFileModel> externalFiles;
    private String commandArg;
    private String cmd;

    public DockerModel() {
        // Initialize with default values except for image name
        this.tag = "latest";
        this.push = false;
        this.buildImage = true;
        this.baseImage = WINDOWS_BUILD ? DockerGenConstants.OPENJDK_8_JRE_WINDOWS_BASE_IMAGE :
                         DockerGenConstants.OPENJDK_8_JRE_ALPINE_BASE_IMAGE;
        this.enableDebug = false;
        this.debugPort = 5005;
        this.setDockerAPIVersion(System.getenv(DOCKER_API_VERSION));
        this.setDockerHost(DockerHost.fromEnv().host());
        this.setDockerCertPath(DockerHost.fromEnv().dockerCertPath());
        
        externalFiles = new HashSet<>();
        commandArg = "";
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isPush() {
        return push;
    }

    public void setPush(boolean push) {
        this.push = push;
    }

    public Set<Integer> getPorts() {
        return ports;
    }

    public void setPorts(Set<Integer> ports) {
        this.ports = ports;
    }

    public String getUberJarFileName() {
        return uberJarFileName;
    }

    public void setUberJarFileName(String uberJarFileName) {
        this.uberJarFileName = uberJarFileName;
    }

    public boolean isService() {
        return isService;
    }

    public void setService(boolean service) {
        isService = service;
    }

    public boolean isBuildImage() {
        return buildImage;
    }

    public void setBuildImage(boolean buildImage) {
        this.buildImage = buildImage;
    }

    public String getBaseImage() {
        return baseImage;
    }

    public void setBaseImage(String baseImage) {
        this.baseImage = baseImage;
    }

    public boolean isEnableDebug() {
        return enableDebug;
    }

    public void setEnableDebug(boolean enableDebug) {
        this.enableDebug = enableDebug;
    }

    public int getDebugPort() {
        return debugPort;
    }

    public void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
    }
    
    public String getDockerAPIVersion() {
        return dockerAPIVersion;
    }
    
    public void setDockerAPIVersion(String dockerAPIVersion) {
        if (null != dockerAPIVersion && !dockerAPIVersion.startsWith("v")) {
            dockerAPIVersion = "v" + dockerAPIVersion;
        }
    
        this.dockerAPIVersion = dockerAPIVersion;
    }
    
    public String getDockerHost() {
        return dockerHost;
    }

    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost.replace("tcp", "https");
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
            addCommandArg(" --b7a.config.file=" + externalFile.getTarget());
        }
    }

    public String getDockerCertPath() {
        return dockerCertPath;
    }

    public void setDockerCertPath(String dockerCertPath) {
        this.dockerCertPath = dockerCertPath;
    }
    
    public String getCommandArg() {
        return commandArg;
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
                .replace("${APP}", this.uberJarFileName)
                .replace("${CONFIG_FILE}", configFile);
    }
    
    public void setCmd(String cmd) {
        this.cmd = cmd;
    }
    
    public boolean isDefaultLinuxBaseImage() {
        return this.baseImage.equals(DockerGenConstants.OPENJDK_8_JRE_ALPINE_BASE_IMAGE);
    }
    
    @Override
    public String toString() {
        return "DockerModel{" +
               "name='" + name + '\'' +
               ", registry='" + registry + '\'' +
               ", tag='" + tag + '\'' +
               ", push=" + push +
               ", username='" + username + '\'' +
               ", password='" + password + '\'' +
               ", buildImage" + "=" + buildImage +
               ", baseImage='" + baseImage + '\'' +
               ", ports=" + ports +
               ", enableDebug=" + enableDebug +
               ", debugPort=" + debugPort +
               ", dockerAPIVersion='" + dockerAPIVersion + '\'' +
               ", dockerHost='" + dockerHost + '\'' +
               ", dockerCertPath='" + dockerCertPath + '\'' +
               ", isService=" + isService +
               ", uberJarFileName='" + uberJarFileName + '\'' +
               ", externalFiles=" + externalFiles +
               ", commandArg='" + commandArg + '\'' +
               ", cmd='" + cmd + '\'' +
               '}';
    }
}
