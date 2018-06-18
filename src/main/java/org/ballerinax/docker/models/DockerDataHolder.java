package org.ballerinax.docker.models;

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
