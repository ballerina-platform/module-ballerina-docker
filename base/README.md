# Ballerina Docker Base Image

## Building Ballerina distribution the image

1. Copy the ballerina runtime distribution zip file to `docker/` folder.
1. Run the following command to build the ballerina distribution docker image by replacing `BALLERINA_DISTRIBUTION_FILE` with name of the runtime distribution copied earlier.

### For linux platform
#### Runtime image
```docker build --no-cache=true --squash --build-arg BALLERINA_DIST=<BALLERINA_DISTRIBUTION_FILE> -t ballerina/ballerina-runtime:<version> ./docker -f runtime.Dockerfile```
#### Tools image
```docker build --no-cache=true --squash --build-arg BALLERINA_DIST=<BALLERINA_DISTRIBUTION_FILE> -t ballerina/ballerina:<version> ./docker -f tools.Dockerfile```

### For windows platform
```docker build --no-cache=true --build-arg BALLERINA_DIST=<BALLERINA_DISTRIBUTION_FILE> -t ballerina/ballerina-runtime:<version> .\docker\ -f .\docker\windows.Dockerfile```

## Building Base image
1. Run the following command to build the base Docker image.
```docker build --squash --no-cache=true -t ballerina/jre8:v1 ./base-image```