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

package org.ballerinax.docker.models;


import org.ballerinax.docker.generator.models.CopyFileModel;
import org.ballerinax.docker.generator.models.DockerModel;

import java.util.HashSet;
import java.util.Set;

/**
 * Docker data holder class.
 */
public class DockerDataHolder {
    private boolean canProcess;
    private Set<Integer> ports;
    private DockerModel dockerModel;
    private Set<CopyFileModel> files;

    public DockerDataHolder() {
        dockerModel = new DockerModel();
        ports = new HashSet<>();
        files = new HashSet<>();
    }

    public Set<Integer> getPorts() {
        return ports;
    }

    public void addPort(int port) {
        this.ports.add(port);
    }

    public DockerModel getDockerModel() {
        return dockerModel;
    }

    public void setDockerModel(DockerModel dockerModel) {
        this.dockerModel = dockerModel;
    }

    public Set<CopyFileModel> getFiles() {
        return files;
    }

    public void addExternalFile(Set<CopyFileModel> files) {
        this.files.addAll(files);
    }

    public boolean isCanProcess() {
        return canProcess;
    }

    public void setCanProcess(boolean canProcess) {
        this.canProcess = canProcess;
    }
}
