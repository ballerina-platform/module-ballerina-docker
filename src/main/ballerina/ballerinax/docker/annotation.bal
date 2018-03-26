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
public annotation <service,endpoint> Config DockerConfiguration;

@Description {value:"External files for docker images"}
@Field {value:"source: source path of the file (in your machine)"}
@Field {value:"target: target path (inside container)"}
@Field {value:"isBallerinaConf: flag whether file is a ballerina config file"}
public struct FileConfig {
    string source;
    string target;
    boolean isBallerinaConf;
}

public struct FileConfigs{
    FileConfig[] files;
}

@Description {value:"External files annotation for Docker Images"}
public annotation <service,endpoint> CopyFiles FileConfigs;
