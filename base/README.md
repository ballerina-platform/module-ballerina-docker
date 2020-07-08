# Ballerina Docker Base Image

## Building Ballerina distribution the image

Run the following command to build the ballerina tools/runtime docker image by replacing `BALLERINA_DISTRIBUTION_FILE` with name of the distribution.

### For linux platform
#### Runtime image
Execute the following command(s)  
```cd base-image```  
```docker build --no-cache=true -t ballerina/jre8:v1 .```
#### Tools image
Copy the ballerina distribution zip file to `docker/` folder and execute the following command(s)    
```cd docker```  
```docker build --no-cache=true --build-arg BALLERINA_DIST=<BALLERINA_DISTRIBUTION_FILE> -t ballerina/ballerina:<version> .```

### For windows platform
#### Tools image
Copy the ballerina distribution zip file to `docker/` folder and execute the following command(s)  
```cd docker```  
```docker build --no-cache=true --build-arg BALLERINA_DIST=<BALLERINA_DISTRIBUTION_FILE> -t ballerina/ballerina-runtime:<version> -f DockerfileWindows .```
