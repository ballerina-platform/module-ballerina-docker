package ballerinax.docker;

@Description {value:"Docker configuration"}
@Field {value:"name: Name of the docker image"}
@Field {value:"registry: Docker registry"}
@Field {value:"tag: Docker image tag"}
@Field {value:"username: Username for docker registry"}
@Field {value:"password: Password for docker registry"}
@Field {value:"baseImage: Base image for docker image building"}
@Field {value:"push: Push to remote registry"}
@Field {value:"buildImage: Build docker image"}
@Field {value:"enableDebug: Enable debug for ballerina program"}
@Field {value:"debugPort: Remote debug port for ballerina program"}
@Field {value:"dockerHost: Docker host IP and docker PORT. ( e.g minikube IP and docker PORT)"}
@Field {value:"dockerCertPath: Docker cert path."}
public struct DockerConfiguration {
    string name;
    string registry;
    string tag;
    string username;
    string password;
    string baseImage;
    boolean push;
    boolean buildImage;
    boolean enableDebug;
    int debugPort;
    string dockerHost;
    string dockerCertPath;
}

@Description {value:"Configurations annotation for Docker"}
public annotation <service> DockerConfig DockerConfiguration;