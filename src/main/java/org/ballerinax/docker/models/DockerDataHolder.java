package org.ballerinax.docker.models;

import java.util.HashSet;
import java.util.Set;

/**
 * Docker data holder class.
 */
public class DockerDataHolder {
    private Set<Integer> ports;
    private DockerModel dockerModel;

    public DockerDataHolder() {
        dockerModel = new DockerModel();
        ports = new HashSet<>();
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
}
