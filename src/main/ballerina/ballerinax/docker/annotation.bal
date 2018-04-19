package ballerinax.docker;

documentation {Docker annotation configuration
    F{{name}} - Name of the docker image
    F{{registry}} - Docker registry url
    F{{tag}} - Docker registry url
    F{{username}} - Docker registry username
    F{{password}} - Docker registry password
    F{{baseImage}} - Base image for Dockerfile
    F{{push}} - Enable pushing docker image to registry
    F{{buildImage}} - Enable docker image build
    F{{enableDebug}} - Enable ballerina debug
    F{{debugPort}} - Ballerina debug port
    F{{dockerHost}} - Docker host IP and docker PORT. ( e.g minikube IP and docker PORT)
    F{{dockerCertPath}} - Docker certificate path
}
public type DockerConfiguration {
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
};

documentation {@docker:Config annotation to configure docker artifact generation
    T{{service}} - Annotation can be attached to a ballerina service
    T{{endpoint}} - Annotation can be attached to a ballerina endpoint
}
public annotation < service, endpoint > Config DockerConfiguration;

documentation {External file type for docker
    F{{source}} - source path of the file (in your machine)
    F{{target}} - target path (inside container)
    F{{isBallerinaConf}} - Flag to specify ballerina config file
}
public type FileConfig {
    string source;
    string target;
    boolean isBallerinaConf;
};

documentation {External File configurations for docker
    F{{files}} - Array of [FileConfig](docker.html#FileConfig)
}
public type FileConfigs {
    FileConfig[] files;
};

documentation {@docker:CopyFile annotation to copy external files to docker image
    T{{service}} - Annotation can be attached to a ballerina service
    T{{endpoint}} - Annotation can be attached to a ballerina endpoint
}
public annotation < service, endpoint > CopyFiles FileConfigs;

documentation {Expose ports for docker
}
public type ExposeConfig {
};


documentation {@docker:Expose annotation to expose ballerina ports
    T{{endpoint}} - Annotation can be attached to a ballerina endpoint
}
public annotation < endpoint > Expose ExposeConfig;
