package org.ballerinax.docker.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Docker context holder class.
 */
public class DockerContext {
    private static DockerContext instance;
    private final Map<String, DockerDataHolder> dockerContext;
    private String currentPackage;

    private DockerContext() {
        dockerContext = new HashMap<>();
    }

    public static DockerContext getInstance() {
        synchronized (DockerDataHolder.class) {
            if (instance == null) {
                instance = new DockerContext();
            }
        }
        return instance;
    }

    public void addDataHolder(String packageID) {
        this.currentPackage = packageID;
        this.dockerContext.put(packageID, new DockerDataHolder());
    }

    public void setCurrentPackage(String packageID) {
        this.currentPackage = packageID;
    }

    public DockerDataHolder getDataHolder() {
        return this.dockerContext.get(this.currentPackage);
    }

    public DockerDataHolder getDataHolder(String packageID) {
        return this.dockerContext.get(packageID);
    }

}
