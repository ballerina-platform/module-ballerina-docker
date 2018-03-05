# Ballerina Docker Base Image

## Building the image

1. Copy Ballerina run time distribution zip file to docker/ folder
1. Run the following command to build the base docker image.

```docker build --no-cache=true --squash --build-arg BALLERINA_DIST=<ballerina-0.962.0.tgz> -t ballerina/b7a:latest ./docker```
